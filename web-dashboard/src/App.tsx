import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "./components/AppLayout";
import FailureDetailPage from "./pages/FailureDetailPage";
import FailureListPage from "./pages/FailureListPage";

function App() {
  return (
    <AppLayout>
      <Routes>
        <Route path="/" element={<Navigate to="/failures" replace />} />
        <Route path="/failures" element={<FailureListPage />} />
        <Route path="/failures/:id" element={<FailureDetailPage />} />
      </Routes>
    </AppLayout>
  );
}

export default App;
