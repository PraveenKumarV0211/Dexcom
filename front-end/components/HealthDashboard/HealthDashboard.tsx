import React, { useState, useEffect } from "react";
import NavBar from "../Navbar/NavBar";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from "chart.js";
import { Line, Bar } from "react-chartjs-2";
import "./HealthDashboard.css";

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

interface HealthEvent {
  type: string;
  units: string;
  date: string;
  data: Record<string, any>;
}

interface DaySummary {
  date: string;
  avgHR: number;
  minHR: number;
  maxHR: number;
  restingHR: number;
  totalSteps: number;
  totalCalories: number;
  hrReadings: number;
}

const HealthDashboard: React.FC = () => {
  const [heartRateData, setHeartRateData] = useState<HealthEvent[]>([]);
  const [stepsData, setStepsData] = useState<HealthEvent[]>([]);
  const [restingHRData, setRestingHRData] = useState<HealthEvent[]>([]);
  const [activeEnergyData, setActiveEnergyData] = useState<HealthEvent[]>([]);
  const [daySummaries, setDaySummaries] = useState<DaySummary[]>([]);
  const [selectedDay, setSelectedDay] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [insight, setInsight] = useState<{ summary: string; anomalies: string; tips: string } | null>(null);
  const [insightLoading, setInsightLoading] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [hrRes, stepsRes, restingRes, energyRes] = await Promise.all([
        fetch("/api/health-events/heart_rate"),
        fetch("/api/health-events/steps"),
        fetch("/api/health-events/resting_heart_rate"),
        fetch("/api/health-events/active_energy"),
      ]);
      const hr: HealthEvent[] = await hrRes.json();
      const steps: HealthEvent[] = await stepsRes.json();
      const resting: HealthEvent[] = await restingRes.json();
      const energy: HealthEvent[] = await energyRes.json();
      setHeartRateData(hr);
      setStepsData(steps);
      setRestingHRData(resting);
      setActiveEnergyData(energy);
      buildSummaries(hr, steps, resting, energy);
      setLoading(false);
    } catch (e) {
      console.error("Failed to fetch health data", e);
      setLoading(false);
    }
  };

  const fetchInsight = async (day: string) => {
    setInsightLoading(true);
    setInsight(null);
    try {
      const res = await fetch(`/api/health-insights/${day}`);
      const data = await res.json();
      const parsed = JSON.parse(data.insight);
      setInsight(parsed);
    } catch (e) {
      console.error("Failed to fetch insight", e);
      setInsight({ summary: "Unable to load insights.", anomalies: "", tips: "" });
    }
    setInsightLoading(false);
  };

  const parseDate = (dateStr: string): Date => {
    return new Date(dateStr.replace(/ -\d{4}$/, ""));
  };

  const getDayKey = (dateStr: string): string => {
    const d = parseDate(dateStr);
    return d.toISOString().split("T")[0];
  };

  const buildSummaries = (hr: HealthEvent[], steps: HealthEvent[], resting: HealthEvent[], energy: HealthEvent[]) => {
    const dayMap: Record<string, { hrVals: number[]; steps: number; restingHR: number; calories: number }> = {};

    const ensureDay = (key: string) => {
      if (!dayMap[key]) dayMap[key] = { hrVals: [], steps: 0, restingHR: 0, calories: 0 };
    };

    hr.forEach((e) => {
      if (!e.date) return;
      const key = getDayKey(e.date);
      ensureDay(key);
      const avg = e.data?.Avg || e.data?.avg;
      if (avg) dayMap[key].hrVals.push(Number(avg));
    });

    steps.forEach((e) => {
      if (!e.date) return;
      const key = getDayKey(e.date);
      ensureDay(key);
      const qty = e.data?.qty;
      if (qty) dayMap[key].steps += Number(qty);
    });

    resting.forEach((e) => {
      if (!e.date) return;
      const key = getDayKey(e.date);
      ensureDay(key);
      const qty = e.data?.qty;
      if (qty) dayMap[key].restingHR = Number(qty);
    });

    energy.forEach((e) => {
      if (!e.date) return;
      const key = getDayKey(e.date);
      ensureDay(key);
      const qty = e.data?.qty;
      if (qty) dayMap[key].calories += Number(qty);
    });

    const summaries: DaySummary[] = Object.entries(dayMap)
      .map(([date, val]) => ({
        date,
        avgHR: val.hrVals.length > 0 ? Math.round(val.hrVals.reduce((a, b) => a + b, 0) / val.hrVals.length) : 0,
        minHR: val.hrVals.length > 0 ? Math.min(...val.hrVals) : 0,
        maxHR: val.hrVals.length > 0 ? Math.max(...val.hrVals) : 0,
        restingHR: val.restingHR,
        totalSteps: Math.round(val.steps),
        totalCalories: Math.round(val.calories),
        hrReadings: val.hrVals.length,
      }))
      .sort((a, b) => b.date.localeCompare(a.date));

    setDaySummaries(summaries);
    if (summaries.length > 0) {
      setSelectedDay(summaries[0].date);
      fetchInsight(summaries[0].date);
    }
  };

  const selectedSummary = daySummaries.find((d) => d.date === selectedDay);

  const getHRChartData = () => {
    if (!selectedDay) return null;
    const dayEvents = heartRateData
      .filter((e) => e.date && getDayKey(e.date) === selectedDay)
      .sort((a, b) => parseDate(a.date).getTime() - parseDate(b.date).getTime());

    return {
      labels: dayEvents.map((e) =>
        parseDate(e.date).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
      ),
      datasets: [
        {
          label: "Heart Rate",
          data: dayEvents.map((e) => e.data?.Avg || e.data?.avg || 0),
          borderColor: "#ef4444",
          backgroundColor: "rgba(239, 68, 68, 0.1)",
          fill: true,
          tension: 0.4,
          pointRadius: 0,
          borderWidth: 2,
        },
      ],
    };
  };

  const getStepsChartData = () => {
    const recent = daySummaries.slice(0, 7).reverse();
    return {
      labels: recent.map((d) => {
        const date = new Date(d.date + "T00:00:00");
        return date.toLocaleDateString([], { weekday: "short", month: "short", day: "numeric" });
      }),
      datasets: [
        {
          label: "Steps",
          data: recent.map((d) => d.totalSteps),
          backgroundColor: "rgba(34, 197, 94, 0.6)",
          borderColor: "#22c55e",
          borderWidth: 1,
          borderRadius: 6,
        },
      ],
    };
  };

  const getCaloriesChartData = () => {
    const recent = daySummaries.slice(0, 7).reverse();
    return {
      labels: recent.map((d) => {
        const date = new Date(d.date + "T00:00:00");
        return date.toLocaleDateString([], { weekday: "short", month: "short", day: "numeric" });
      }),
      datasets: [
        {
          label: "Calories",
          data: recent.map((d) => d.totalCalories),
          backgroundColor: "rgba(251, 146, 60, 0.6)",
          borderColor: "#fb923c",
          borderWidth: 1,
          borderRadius: 6,
        },
      ],
    };
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: "#1e293b",
        titleColor: "#f9fafb",
        bodyColor: "#94a3b8",
        borderColor: "rgba(255,255,255,0.1)",
        borderWidth: 1,
      },
    },
    scales: {
      x: {
        ticks: { color: "#64748b", maxTicksLimit: 8 },
        grid: { color: "rgba(255,255,255,0.03)" },
      },
      y: {
        ticks: { color: "#64748b" },
        grid: { color: "rgba(255,255,255,0.03)" },
      },
    },
  };

  if (loading) {
    return (
      <div className="homepage-container">
        <NavBar />
        <div className="main-content">
          <div className="health-loading">Loading health data...</div>
        </div>
      </div>
    );
  }

  const hrChart = getHRChartData();
  const stepsChart = getStepsChartData();
  const caloriesChart = getCaloriesChartData();

  return (
    <div className="homepage-container">
      <NavBar />
      <div className="main-content">
        <h2 className="health-title">Health Metrics</h2>

        {/* Day Selector */}
        <div className="day-selector">
          {daySummaries.slice(0, 7).map((day) => (
            <div
              key={day.date}
              className={`day-pill ${selectedDay === day.date ? "active" : ""}`}
              onClick={() => {
                setSelectedDay(day.date);
                fetchInsight(day.date);
              }}
            >
              <span className="day-pill-label">
                {new Date(day.date + "T00:00:00").toLocaleDateString([], { weekday: "short" })}
              </span>
              <span className="day-pill-date">
                {new Date(day.date + "T00:00:00").toLocaleDateString([], { month: "short", day: "numeric" })}
              </span>
            </div>
          ))}
        </div>

        {/* Metric Cards */}
        {selectedSummary && (
          <div className="metric-cards">
            <div className="metric-card hr-card">
              <div className="metric-card-header">
                <span className="metric-dot hr-dot"></span>
                <span className="metric-label">Heart Rate</span>
              </div>
              <div className="metric-value">{selectedSummary.avgHR}</div>
              <div className="metric-unit">avg bpm</div>
              <div className="metric-range">
                <span>{selectedSummary.minHR} min</span>
                <span>{selectedSummary.maxHR} max</span>
              </div>
            </div>

            <div className="metric-card resting-card">
              <div className="metric-card-header">
                <span className="metric-dot resting-dot"></span>
                <span className="metric-label">Resting HR</span>
              </div>
              <div className="metric-value">{selectedSummary.restingHR || "—"}</div>
              <div className="metric-unit">bpm</div>
            </div>

            <div className="metric-card steps-card">
              <div className="metric-card-header">
                <span className="metric-dot steps-dot"></span>
                <span className="metric-label">Steps</span>
              </div>
              <div className="metric-value">{selectedSummary.totalSteps.toLocaleString()}</div>
              <div className="metric-unit">total</div>
            </div>

            <div className="metric-card calories-card">
              <div className="metric-card-header">
                <span className="metric-dot calories-dot"></span>
                <span className="metric-label">Active Energy</span>
              </div>
              <div className="metric-value">{selectedSummary.totalCalories.toLocaleString()}</div>
              <div className="metric-unit">kcal</div>
            </div>
          </div>
        )}

        {/* AI Insights */}
        <div className="insight-card">
          <div className="insight-header">
            <span className="insight-icon">✦</span>
            <span className="insight-title">AI Health Insights</span>
          </div>
          {insightLoading ? (
            <div className="insight-loading">
              <div className="insight-loading-dot"></div>
              <span>Analyzing your health data...</span>
            </div>
          ) : insight ? (
            <div className="insight-body">
              <div className="insight-section">
                <div className="insight-section-label">Summary</div>
                <div className="insight-section-text">{insight.summary}</div>
              </div>
              <div className="insight-section">
                <div className="insight-section-label">Anomalies</div>
                <div className="insight-section-text">{insight.anomalies}</div>
              </div>
              <div className="insight-section">
                <div className="insight-section-label">Tips</div>
                <div className="insight-section-text">{insight.tips}</div>
              </div>
            </div>
          ) : null}
        </div>

        {/* Charts */}
        <div className="health-charts">
          <div className="health-chart-box wide">
            <h3 className="chart-title">Heart Rate Timeline</h3>
            <div className="chart-container">
              {hrChart && hrChart.labels.length > 0 ? (
                <Line data={hrChart} options={chartOptions} />
              ) : (
                <div className="no-data">No heart rate data for this day</div>
              )}
            </div>
          </div>
        </div>

        <div className="health-charts">
          <div className="health-chart-box">
            <h3 className="chart-title">Daily Steps (Last 7 Days)</h3>
            <div className="chart-container">
              {stepsChart && stepsChart.labels.length > 0 ? (
                <Bar data={stepsChart} options={chartOptions} />
              ) : (
                <div className="no-data">No step data available</div>
              )}
            </div>
          </div>

          <div className="health-chart-box">
            <h3 className="chart-title">Active Energy (Last 7 Days)</h3>
            <div className="chart-container">
              {caloriesChart && caloriesChart.labels.length > 0 ? (
                <Bar data={caloriesChart} options={chartOptions} />
              ) : (
                <div className="no-data">No energy data available</div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HealthDashboard;