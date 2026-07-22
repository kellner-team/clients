import shared
import SwiftUI
import UIPilot

/// Helper view which sets up everything needed for previewing content
public struct PreviewView<Content: View>: View {
    @StateObject
    private var navigator = UIPilot<Screen>(initial: CommonApp.shared.getNextRootScreen(), debug: true)

    private let withUIPilot: Bool

    @ViewBuilder
    private let content: Content

    public init(withUIPilot: Bool = true, @ViewBuilder content: () -> Content) {
        print("preview setup")
        WRCore.setup()

        self.withUIPilot = withUIPilot
        self.content = content()
        print("preview done")
    }

    public var body: some View {
        ZStack {
            // UIPilotHost is only needed when actually running a preview. Referencing it
            // in Release causes a linker error: the optimizer specializes UIPilotHost's
            // body into this module and references UIPilot's internal ViewGenerator
            // metadata, which the library does not export.
            #if DEBUG
            if withUIPilot {
                UIPilotHost(navigator) { _ in
                    content
                }
            } else {
                content
            }
            #else
            content
            #endif
        }
    }
}
