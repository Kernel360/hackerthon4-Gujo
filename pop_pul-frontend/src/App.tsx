// src/App.tsx
import "./App.css";
import { AuthProvider } from "./hooks/useUser";
import AppRoutes from "./routes";

function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
}

export default App;
