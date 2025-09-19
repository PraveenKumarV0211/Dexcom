import { useState } from "react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import HomePage from "../components/HomePage";
import ReportPage from "../components/ReportsPage/ReportPage";
import "./App.css";

function App() {
  const [count, setCount] = useState(0);

  return (
    <>
      <Router>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/report"  element={<ReportPage />} />
        </Routes>
      </Router>
    </>
  );
}

export default App;