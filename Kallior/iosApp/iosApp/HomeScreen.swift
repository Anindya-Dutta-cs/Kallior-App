import SwiftUI

// MARK: - Colors
struct KalliorColors {
    static let secondaryBackground = Color(red: 62 / 255, green: 62 / 255, blue: 62 / 255) // #3E3E3E
    static let primaryLayer = Color(red: 44 / 255, green: 44 / 255, blue: 44 / 255) // #2C2C2C
    static let accentOrange = Color(red: 255 / 255, green: 179 / 255, blue: 112 / 255) // #FFB370
    static let mutedText = Color(red: 172 / 255, green: 172 / 255, blue: 172 / 255) // #ACACAC
    static let normalText = Color.white // #FFFFFF
}

// MARK: - HomeScreen
struct HomeScreen: View {
    @StateObject private var viewModel = HomeViewModel()
    @State private var scrollOffset: CGFloat = 0
    
    var body: some View {
        NavigationStack {
            ZStack(alignment: .top) {
                KalliorColors.secondaryBackground.ignoresSafeArea()
                
                // Track Scroll Offset
                ScrollView(.vertical, showsIndicators: false) {
                    VStack(spacing: 0) {
                        // GeometryReader to calculate offset
                        GeometryReader { proxy in
                            Color.clear.preference(
                                key: ScrollOffsetPreferenceKey.self,
                                value: proxy.frame(in: .named("scroll")).minY
                            )
                        }
                        .frame(height: 0)
                        
                        // Transparent spacer to reveal the background radar chart
                        Spacer()
                            .frame(height: 350)
                        
                        // Primary Layer
                        primaryLayerContent
                    }
                }
                .coordinateSpace(name: "scroll")
                .onPreferenceChange(ScrollOffsetPreferenceKey.self) { value in
                    self.scrollOffset = value
                }
                
                // Secondary Layer (Radar Chart & Nav)
                // Positioned absolutely at the top, reacting to scrollOffset
                secondaryLayerContent
            }
            .navigationBarHidden(true)
        }
    }
    
    // MARK: - Secondary Layer Content
    private var secondaryLayerContent: some View {
        VStack {
            // Top Navigation
            HStack {
                Button(action: {}) {
                    Image(systemName: "line.3.horizontal")
                        .foregroundColor(.white)
                        .padding()
                        .background(KalliorColors.primaryLayer)
                        .clipShape(Circle())
                }
                
                Spacer()
                
                Button(action: {}) {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(KalliorColors.primaryLayer)
                        .frame(width: 44, height: 44)
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 16)
            
            // Radar Chart with Parallax Effect
            let parallaxScale = max(0.8, min(1.0, 1.0 + (scrollOffset / 1000)))
            let parallaxOpacity = max(0.0, min(1.0, 1.0 + (scrollOffset / 500)))
            
            RadarChartView(
                scores: viewModel.radarScores.values,
                axisIcons: ["target", "eye", "shield", "heart", "scope"],
                accentColor: KalliorColors.accentOrange
            )
            .frame(width: 250, height: 250)
            .scaleEffect(parallaxScale)
            .opacity(parallaxOpacity)
            .padding(.top, 20)
            
            Spacer()
        }
        .ignoresSafeArea(.container, edges: .bottom)
        // Ensure it sits behind the scroll view content visually but is fixed
        .zIndex(-1)
    }
    
    // MARK: - Primary Layer Content
    private var primaryLayerContent: some View {
        VStack(spacing: 32) {
            // Tasks Section
            SectionView(
                title: "Tasks",
                emptyText: "Click on + to add a\nnew task",
                buttonIcon: "plus",
                onButtonTap: {
                    viewModel.addTask(title: "New Task", categoryIndex: 0)
                }
            )
            
            // Remainders Section
            SectionView(
                title: "Remainders",
                emptyText: "No remainders\nneeded? You're a\ntotal integer!",
                buttonIcon: "plus",
                onButtonTap: {}
            )
            
            // Badges Section
            NavigationLink(destination: BadgesScreen()) {
                SectionView(
                    title: "Badges",
                    emptyText: "Wow, such empty!",
                    buttonIcon: "chevron.right",
                    onButtonTap: {}
                )
            }
            .buttonStyle(PlainButtonStyle())
            
            Spacer().frame(height: 50)
        }
        .padding(.horizontal, 24)
        .padding(.top, 40)
        .background(
            KalliorColors.primaryLayer
                .clipShape(RoundedCornerShape(radius: 40, corners: [.topLeft, .topRight]))
                .ignoresSafeArea(edges: .bottom)
        )
    }
}

// MARK: - Subcomponents

struct SectionView: View {
    let title: String
    let emptyText: String
    let buttonIcon: String
    let onButtonTap: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(title)
                .font(.system(size: 24, weight: .regular, design: .serif))
                .foregroundColor(KalliorColors.normalText)
            
            ZStack(alignment: .topTrailing) {
                RoundedRectangle(cornerRadius: 20)
                    .fill(KalliorColors.secondaryBackground)
                    .frame(height: 140)
                
                Text(emptyText)
                    .font(.system(size: 14))
                    .foregroundColor(KalliorColors.mutedText)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                
                Button(action: onButtonTap) {
                    ZStack {
                        Circle()
                            .fill(KalliorColors.accentOrange)
                            .frame(width: 44, height: 44)
                        Image(systemName: buttonIcon)
                            .foregroundColor(.white)
                            .font(.system(size: 20, weight: .bold))
                    }
                }
                .offset(x: 10, y: -10)
            }
        }
    }
}

// MARK: - Helpers

struct ScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

struct RoundedCornerShape: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(roundedRect: rect, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
        return Path(path.cgPath)
    }
}
