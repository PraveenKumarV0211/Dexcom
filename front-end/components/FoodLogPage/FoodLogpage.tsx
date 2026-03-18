import React, { useState, useEffect } from "react";
import NavBar from "../Navbar/NavBar";
import "./FoodLogPage.css";
import { UtensilsCrossed, Clock, Plus } from "lucide-react";

interface FoodLogEntry {
  id: string;
  timestamp: string;
  foodName: string;
  mealType: string;
  portionSize: string;
  source: string;
  notes: string;
  carbs?: number;
  protein?: number;
  fiber?: number;
}

const FoodLogPage: React.FC = () => {
  const [foodName, setFoodName] = useState("");
  const [mealType, setMealType] = useState("lunch");
  const [portionSize, setPortionSize] = useState("");
  const [source, setSource] = useState("");
  const [notes, setNotes] = useState("");
  const [timestamp, setTimestamp] = useState("");
  const [carbs, setCarbs] = useState<string>("");
  const [protein, setProtein] = useState<string>("");
  const [fiber, setFiber] = useState<string>("");
  const [showNutrition, setShowNutrition] = useState(false);
  const [recentLogs, setRecentLogs] = useState<FoodLogEntry[]>([]);

  useEffect(() => {
    fetchRecentLogs();
  }, []);

  const fetchRecentLogs = async () => {
    try {
      const response = await fetch(`${import.meta.env.VITE_FOOD_LOG_API}`);
      if (response.ok) {
        const data = await response.json();
        setRecentLogs(data);
      }
    } catch (err) {
      console.error("Error fetching food logs:", err);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!foodName || !portionSize) {
      alert("Please provide food name and portion size.");
      return;
    }

    const body: any = {
      foodName,
      mealType,
      portionSize,
      source,
      notes,
    };

    if (timestamp) body.timestamp = new Date(timestamp).toISOString();
    if (carbs) body.carbs = parseFloat(carbs);
    if (protein) body.protein = parseFloat(protein);
    if (fiber) body.fiber = parseFloat(fiber);

    try {
      const response = await fetch(`${import.meta.env.VITE_FOOD_LOG_API}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      if (response.ok) {
        setFoodName("");
        setPortionSize("");
        setSource("");
        setNotes("");
        setTimestamp("");
        setCarbs("");
        setProtein("");
        setFiber("");
        fetchRecentLogs();
      } else {
        alert("Failed to add food log.");
      }
    } catch (err) {
      console.error(err);
      alert("Error adding food log.");
    }
  };

  const formatTime = (ts: string) => {
    const date = new Date(ts);
    return date.toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    });
  };

  const getMealColor = (meal: string) => {
    switch (meal) {
      case "breakfast": return "#f59e0b";
      case "lunch": return "#3b82f6";
      case "dinner": return "#8b5cf6";
      case "snack": return "#10b981";
      default: return "#6b7280";
    }
  };

  return (
    <div className="foodlog-layout">
      <NavBar />
      <div className="foodlog-content">
        <div className="foodlog-grid">
          {/* Left: Form */}
          <div className="foodlog-form-card">
            <div className="form-header">
              <UtensilsCrossed size={20} />
              <h2>Log Food</h2>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="form-row">
                <div className="form-group full">
                  <label>Food Name *</label>
                  <input
                    type="text"
                    value={foodName}
                    onChange={(e) => setFoodName(e.target.value)}
                    placeholder="e.g. Chicken Biryani"
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Meal Type</label>
                  <select value={mealType} onChange={(e) => setMealType(e.target.value)}>
                    <option value="breakfast">Breakfast</option>
                    <option value="lunch">Lunch</option>
                    <option value="dinner">Dinner</option>
                    <option value="snack">Snack</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Portion Size *</label>
                  <input
                    type="text"
                    value={portionSize}
                    onChange={(e) => setPortionSize(e.target.value)}
                    placeholder="e.g. 1 plate"
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Source</label>
                  <input
                    type="text"
                    value={source}
                    onChange={(e) => setSource(e.target.value)}
                    placeholder="e.g. homemade, Zafran"
                  />
                </div>
                <div className="form-group">
                  <label>Date & Time</label>
                  <input
                    type="datetime-local"
                    value={timestamp}
                    onChange={(e) => setTimestamp(e.target.value)}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group full">
                  <label>Notes</label>
                  <input
                    type="text"
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    placeholder="e.g. with raita and salad"
                  />
                </div>
              </div>

              <button
                type="button"
                className="nutrition-toggle"
                onClick={() => setShowNutrition(!showNutrition)}
              >
                {showNutrition ? "Hide" : "Add"} Nutrition Info (optional)
              </button>

              {showNutrition && (
                <div className="form-row nutrition-row">
                  <div className="form-group">
                    <label>Carbs (g)</label>
                    <input
                      type="number"
                      value={carbs}
                      onChange={(e) => setCarbs(e.target.value)}
                      placeholder="0"
                    />
                  </div>
                  <div className="form-group">
                    <label>Protein (g)</label>
                    <input
                      type="number"
                      value={protein}
                      onChange={(e) => setProtein(e.target.value)}
                      placeholder="0"
                    />
                  </div>
                  <div className="form-group">
                    <label>Fiber (g)</label>
                    <input
                      type="number"
                      value={fiber}
                      onChange={(e) => setFiber(e.target.value)}
                      placeholder="0"
                    />
                  </div>
                </div>
              )}

              <button type="submit" className="submit-btn">
                <Plus size={18} />
                Add Food Log
              </button>
            </form>
          </div>

          {/* Right: Recent Logs */}
          <div className="foodlog-recent-card">
            <div className="form-header">
              <Clock size={20} />
              <h2>Recent Logs</h2>
            </div>

            <div className="recent-logs-scroll">
              {recentLogs.length > 0 ? (
                recentLogs.map((log) => (
                  <div key={log.id} className="log-item">
                    <div className="log-item-top">
                      <span className="log-food-name">{log.foodName}</span>
                      <span
                        className="log-meal-badge"
                        style={{ backgroundColor: getMealColor(log.mealType) }}
                      >
                        {log.mealType}
                      </span>
                    </div>
                    <div className="log-item-details">
                      <span>{log.portionSize}</span>
                      {log.source && <span>· {log.source}</span>}
                      {log.timestamp && <span>· {formatTime(log.timestamp)}</span>}
                    </div>
                    {(log.carbs || log.protein || log.fiber) && (
                      <div className="log-item-nutrition">
                        {log.carbs && <span>Carbs: {log.carbs}g</span>}
                        {log.protein && <span>Protein: {log.protein}g</span>}
                        {log.fiber && <span>Fiber: {log.fiber}g</span>}
                      </div>
                    )}
                    {log.notes && <div className="log-item-notes">{log.notes}</div>}
                  </div>
                ))
              ) : (
                <div className="no-logs">No food logs yet</div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FoodLogPage;