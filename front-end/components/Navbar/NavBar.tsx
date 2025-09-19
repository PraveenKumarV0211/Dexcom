import React from "react";
import { Home, BarChart2, Calendar } from "lucide-react";
import "./NavBar.css"

const Navbar: React.FC = () => {
  return (
    <nav className="navbar">
      <div className="nav-item">
        <Home className="icon" />
        <span className="tooltip">Home</span>
      </div>

     
      <div className="nav-item">
        <Calendar className="icon" />
        <span className="tooltip">Report</span>
      </div>
    </nav>
  );
};

export default Navbar;
