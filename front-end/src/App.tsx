import { useState } from "react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import HomePage from "../components/HomePage";
import ReportPage from "../components/ReportsPage/ReportPage";
import FoodLogPage from "../components/FoodLogPage/FoodLogpage";
import ChatPage from "../components/ChatPage/ChatPage"
import HealthDashboard from "../components/HealthDashboard/HealthDashboard";
import "./App.css";

function App() {
  const [count, setCount] = useState(0);

  return (
    <>
      <Router>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/report"  element={<ReportPage />} />
          <Route path="/food-log" element={<FoodLogPage />} />
          <Route path="/chat" element={<ChatPage />} />
          <Route path="/health" element={<HealthDashboard />} />
        </Routes>
      </Router>
    </>
  );
}

export default App;