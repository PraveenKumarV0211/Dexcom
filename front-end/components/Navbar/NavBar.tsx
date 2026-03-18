import React from "react";
import { Home, Calendar, UtensilsCrossed, MessageCircle } from "lucide-react";
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

      <div className="nav-item" onClick={() => navigate("/food-log")}>
        <UtensilsCrossed className="icon" />
        <span className="tooltip">Food Log</span>
      </div>

      <div className="nav-item" onClick={() => navigate("/chat")}>
        <MessageCircle className="icon" />
        <span className="tooltip">Chat</span>
      </div>

    </nav>
  );
};

export default Navbar;
