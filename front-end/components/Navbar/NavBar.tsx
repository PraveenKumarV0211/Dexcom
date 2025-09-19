import React from "react";
import { Home, Calendar } from "lucide-react";
import { useNavigate } from "react-router-dom";
import "./NavBar.css"

const Navbar: React.FC = () => {
  const navigate = useNavigate();
  return (
    <nav className="navbar">
      <div className="nav-item" onClick={() => navigate("/")}>
        <Home className="icon" />
        <span className="tooltip">Home</span>
      </div>

     
      <div className="nav-item" onClick={() => navigate("/report")}>
        <Calendar className="icon" />
        <span className="tooltip">Report</span>
      </div>
    </nav>
  );
};

export default Navbar;
