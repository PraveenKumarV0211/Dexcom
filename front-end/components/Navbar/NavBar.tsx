import React from "react";
import { Home, Calendar, UtensilsCrossed, MessageCircle, Activity } from "lucide-react";
import { useNavigate, useLocation } from "react-router-dom";
import "./NavBar.css";

const Navbar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = [
    { path: "/", icon: Home, label: "Home" },
    { path: "/report", icon: Calendar, label: "Report" },
    { path: "/food-log", icon: UtensilsCrossed, label: "Food Log" },
    { path: "/chat", icon: MessageCircle, label: "Chat" },
    { path: "/health", icon: Activity, label: "Health" },
  ];

  return (
    <nav className="navbar">
      {navItems.map((item) => (
        <div
          key={item.path}
          className={`nav-item ${location.pathname === item.path ? "active" : ""}`}
          onClick={() => navigate(item.path)}
        >
          <item.icon className="icon" />
          <span className="nav-label">{item.label}</span>
        </div>
      ))}
    </nav>
  );
};

export default Navbar;