import Foundation
import Combine
import ComposeApp

// MARK: - Swift Bridge Types
// These pure-Swift types isolate SwiftUI views from KMP/Obj-C bridging details.

/// Mirrors a Kotlin `kallos.model.Task` without conflicting with `Swift.Task`.
struct KalliorTask: Identifiable {
    let id: String
    var title: String
    var categoryName: String
    var statusName: String
    var estimateMinutes: Int
}

/// Mirrors `kallos.domain.RadarScores` for the radar chart.
struct KalliorRadarScores {
    var consistency: Double = 0
    var discipline: Double = 0
    var focus: Double = 0
    var health: Double = 0
    var resilience: Double = 0

    static let zero = KalliorRadarScores()

    /// Values ordered to match `RadarChartView` vertex positions:
    /// [Focus (top), Discipline (upper-right), Health (lower-right),
    ///  Resilience (lower-left), Consistency (upper-left)]
    var values: [Double] {
        [focus, discipline, health, resilience, consistency]
    }
}

// MARK: - HomeViewModel

/// SwiftUI ViewModel that bridges the Kotlin `GameViewModel` backend.
///
/// All KMP types stay inside this class; SwiftUI views consume only the
/// pure-Swift `Kallior*` bridge types above.
///
/// > **Note:** If the Kotlin class names differ in the generated Obj-C
/// > header (e.g. a prefixed name), adjust the references inside `sync()`
/// > accordingly.
final class HomeViewModel: ObservableObject {

    private let gameVM: GameViewModel

    @Published var tasks: [KalliorTask] = []
    @Published var radarScores = KalliorRadarScores.zero
    @Published var statusMessage: String? = nil
    @Published var showingAddTask = false

    /// Human-readable category names matching `kallos.model.Category` order.
    static let categoryNames = ["Exercise", "Study / Work", "Meditation", "Diet", "Other"]

    init() {
        self.gameVM = GameViewModel()
        sync()
    }

    // MARK: - Sync from Kotlin VM

    /// Reads current state from the Kotlin `GameViewModel` and publishes it
    /// as pure-Swift types for SwiftUI consumption.
    func sync() {
        // --- Tasks ---
        // `SnapshotStateList<Task>` implements `MutableList` which Kotlin/Native
        //  bridges to `NSMutableArray`. We iterate and cast each element.
        var bridged: [KalliorTask] = []
        if let array = gameVM.tasks as? NSArray {
            for item in array {
                // Module-qualify to avoid collision with `Swift.Task`.
                guard let task = item as? ComposeApp.Task else { continue }
                bridged.append(KalliorTask(
                    id: task.id,
                    title: task.title,
                    categoryName: task.category.displayName,
                    statusName: task.status.name,
                    estimateMinutes: Int(task.estimateMinutes)
                ))
            }
        }
        self.tasks = bridged

        // --- Radar scores ---
        let s = gameVM.userScores
        self.radarScores = KalliorRadarScores(
            consistency: s.consistency,
            discipline: s.discipline,
            focus: s.focus,
            health: s.health,
            resilience: s.resilience
        )

        self.statusMessage = gameVM.statusMessage
    }

    // MARK: - Task Actions

    /// Adds a task using a category index that maps to `kallos.model.Category`.
    func addTask(title: String, categoryIndex: Int) {
        let category = mapCategory(index: categoryIndex)
        gameVM.addTask(title: title, category: category)
        sync()
    }

    func deleteTask(id: String) {
        gameVM.deleteTask(taskId: id)
        sync()
    }

    func confirmTask(id: String) {
        gameVM.confirmTask(taskId: id)
        sync()
    }

    func completeTask(id: String) {
        gameVM.completeTask(taskId: id, proof: nil)
        sync()
    }

    func clearStatus() {
        gameVM.clearStatusMessage()
        statusMessage = nil
    }

    // MARK: - Private Helpers

    private func mapCategory(index: Int) -> Category {
        switch index {
        case 0:  return .exercise
        case 1:  return .studyOrWork
        case 2:  return .meditation
        case 3:  return .diet
        default: return .other
        }
    }
}
