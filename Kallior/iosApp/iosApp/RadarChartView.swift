import SwiftUI

// MARK: - RadarChartView

/// A 5-axis radar (spider-web) chart drawn with `Path`.
///
/// Renders concentric pentagon rings, axis lines, and a filled data polygon.
/// SF Symbol icons are placed at each vertex beyond the outer ring.
struct RadarChartView: View {

    /// Five score values in range 0…100, ordered to match vertex positions:
    /// [top, upper-right, lower-right, lower-left, upper-left].
    let scores: [Double]

    /// Five SF Symbol names matching the vertex order.
    let axisIcons: [String]

    /// Accent color for the data polygon and vertex icons.
    let accentColor: Color

    private let axisCount = 5
    private let ringCount = 4

    var body: some View {
        GeometryReader { geo in
            let size = min(geo.size.width, geo.size.height)
            let center = CGPoint(x: geo.size.width / 2, y: geo.size.height / 2)
            let maxRadius = size * 0.34

            ZStack {
                // Concentric pentagon rings
                ForEach(1...ringCount, id: \.self) { ring in
                    let fraction = CGFloat(ring) / CGFloat(ringCount)
                    pentagonPath(center: center, radius: maxRadius * fraction)
                        .stroke(Color.white.opacity(0.12), lineWidth: 0.8)
                }

                // Axis lines from center to each vertex
                ForEach(0..<axisCount, id: \.self) { i in
                    Path { path in
                        path.move(to: center)
                        path.addLine(to: vertexPoint(index: i, center: center, radius: maxRadius))
                    }
                    .stroke(Color.white.opacity(0.12), lineWidth: 0.8)
                }

                // Data polygon – filled
                dataPolygon(center: center, maxRadius: maxRadius)
                    .fill(accentColor.opacity(0.15))

                // Data polygon – stroked
                dataPolygon(center: center, maxRadius: maxRadius)
                    .stroke(accentColor.opacity(0.7), lineWidth: 1.5)

                // Data-point circles
                ForEach(0..<axisCount, id: \.self) { i in
                    let v = normalizedScore(at: i)
                    let pt = vertexPoint(index: i, center: center, radius: maxRadius * v)
                    Circle()
                        .fill(accentColor)
                        .frame(width: 5, height: 5)
                        .position(pt)
                }

                // Vertex icons
                ForEach(0..<axisCount, id: \.self) { i in
                    let iconR = maxRadius + 28
                    let pt = vertexPoint(index: i, center: center, radius: iconR)
                    Image(systemName: safeIcon(at: i))
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(accentColor)
                        .position(pt)
                }
            }
        }
        .aspectRatio(1, contentMode: .fit)
    }

    // MARK: - Geometry Helpers

    /// Angle for vertex `index`: `(2π/5)·i`, so each axis is evenly spaced and every
    /// vertex sits exactly on its axis line.
    private func angle(for index: Int) -> CGFloat {
        CGFloat(index) * (2 * .pi / CGFloat(axisCount))
    }

    private func vertexPoint(index: Int, center: CGPoint, radius: CGFloat) -> CGPoint {
        let a = angle(for: index)
        return CGPoint(x: center.x + radius * cos(a),
                       y: center.y + radius * sin(a))
    }

    private func normalizedScore(at index: Int) -> CGFloat {
        guard scores.indices.contains(index) else { return 0 }
        return CGFloat(max(0, min(scores[index], 100))) / 100.0
    }

    private func safeIcon(at index: Int) -> String {
        axisIcons.indices.contains(index) ? axisIcons[index] : "circle"
    }

    // MARK: - Path Builders

    private func pentagonPath(center: CGPoint, radius: CGFloat) -> Path {
        Path { path in
            for i in 0..<axisCount {
                let pt = vertexPoint(index: i, center: center, radius: radius)
                i == 0 ? path.move(to: pt) : path.addLine(to: pt)
            }
            path.closeSubpath()
        }
    }

    private func dataPolygon(center: CGPoint, maxRadius: CGFloat) -> Path {
        Path { path in
            for i in 0..<axisCount {
                let v = normalizedScore(at: i)
                let pt = vertexPoint(index: i, center: center, radius: maxRadius * v)
                i == 0 ? path.move(to: pt) : path.addLine(to: pt)
            }
            path.closeSubpath()
        }
    }
}
