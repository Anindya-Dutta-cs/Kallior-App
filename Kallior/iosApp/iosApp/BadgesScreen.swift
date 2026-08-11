import SwiftUI

/// Placeholder screen for the Badges feature.
struct BadgesScreen: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            Color(red: 62 / 255, green: 62 / 255, blue: 62 / 255)
                .ignoresSafeArea()

            VStack(spacing: 16) {
                Image(systemName: "trophy.fill")
                    .font(.system(size: 48))
                    .foregroundColor(Color(red: 255 / 255, green: 179 / 255, blue: 112 / 255))

                Text("Badges")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.white)

                Text("Coming soon…")
                    .font(.system(size: 16))
                    .foregroundColor(Color(red: 172 / 255, green: 172 / 255, blue: 172 / 255))
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }
}
