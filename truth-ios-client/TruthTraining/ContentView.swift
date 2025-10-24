import SwiftUI

struct ContentView: View {
    @StateObject private var truthCore = TruthCore()
    @State private var jsonInput = """
    {
        "action": "ping",
        "timestamp": 1640995200
    }
    """
    @State private var response = ""
    @State private var isProcessing = false
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text("Truth Training iOS Client")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                
                VStack(alignment: .leading, spacing: 8) {
                    Text("JSON Input:")
                        .font(.headline)
                    
                    TextEditor(text: $jsonInput)
                        .frame(height: 120)
                        .padding(8)
                        .background(Color.gray.opacity(0.1))
                        .cornerRadius(8)
                }
                
                Button(action: processJson) {
                    HStack {
                        if isProcessing {
                            ProgressView()
                                .scaleEffect(0.8)
                        }
                        Text(isProcessing ? "Processing..." : "Process JSON")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(8)
                }
                .disabled(isProcessing)
                
                if !response.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Response:")
                            .font(.headline)
                        
                        ScrollView {
                            Text(response)
                                .font(.system(.body, design: .monospaced))
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color.gray.opacity(0.1))
                                .cornerRadius(8)
                        }
                        .frame(maxHeight: 200)
                    }
                }
                
                Spacer()
            }
            .padding()
            .navigationTitle("Truth Core")
        }
    }
    
    private func processJson() {
        isProcessing = true
        response = ""
        
        DispatchQueue.global(qos: .userInitiated).async {
            let result = truthCore.processJson(jsonInput)
            
            DispatchQueue.main.async {
                self.response = result ?? "Error processing JSON"
                self.isProcessing = false
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
