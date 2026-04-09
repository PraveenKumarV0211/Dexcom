import React from "react";
import "./DailyGoals.css";

interface DailyGoalsProps {
    steps: number;
    calories: number;
    hrReadings: number;
    activeMinutes: number;
}

const DailyGoals: React.FC<DailyGoalsProps> = ({ steps, calories, activeMinutes }) => {
    const goals = [
        { label: "Steps", value: steps, target: 10000, color: "#4ade80", icon: "👟" },
        { label: "Calories", value: calories, target: 500, color: "#fb923c", icon: "🔥" },
        { label: "Active Min", value: activeMinutes, target: 30, color: "#818cf8", icon: "⚡" },
    ];

    const radius = 28;
    const circumference = 2 * Math.PI * radius;

    return (
        <div className="dg-container">
            <div className="dg-title-row">
                <span className="dg-title">Daily Goals</span>
            </div>
            <div className="dg-rings">
                {goals.map((g, i) => {
                    const pct = Math.min(g.value / g.target, 1);
                    const offset = circumference - pct * circumference;
                    const displayPct = Math.round(pct * 100);

                    return (
                        <div className="dg-ring-item" key={i}>
                            <div className="dg-ring-wrap">
                                <svg viewBox="0 0 70 70" className="dg-ring-svg">
                                    <circle cx="35" cy="35" r={radius} fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="5" />
                                    <circle
                                        cx="35" cy="35" r={radius}
                                        fill="none"
                                        stroke={g.color}
                                        strokeWidth="5"
                                        strokeLinecap="round"
                                        strokeDasharray={circumference}
                                        strokeDashoffset={offset}
                                        transform="rotate(-90 35 35)"
                                        className="dg-ring-fill"
                                    />
                                </svg>
                                <div className="dg-ring-center">
                                    <span className="dg-ring-icon">{g.icon}</span>
                                </div>
                            </div>
                            <div className="dg-ring-info">
                                <div className="dg-ring-pct" style={{ color: g.color }}>{displayPct}%</div>
                                <div className="dg-ring-label">{g.label}</div>
                                <div className="dg-ring-detail">{g.value.toLocaleString()} / {g.target.toLocaleString()}</div>
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default DailyGoals;