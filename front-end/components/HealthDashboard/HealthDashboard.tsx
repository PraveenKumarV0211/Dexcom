import React, { useState, useEffect } from "react";
import NavBar from "../Navbar/NavBar";
import HealthScore from "./HealthScore";
import DailyGoals from "./DailyGoals";
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

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend, Filler);

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
  totalDistance: number;
  exerciseTime: number;
}

interface DayData {
  summary: DaySummary;
  hrEvents: HealthEvent[];
}

const HealthDashboard: React.FC = () => {
  const [availableDays, setAvailableDays] = useState<string[]>([]);
  const [selectedDay, setSelectedDay] = useState<string | null>(null);
  const [dayDataCache, setDayDataCache] = useState<Record<string, DayData>>({});
  const [loading, setLoading] = useState(true);
  const [dayLoading, setDayLoading] = useState(false);
  const [insight, setInsight] = useState<{ summary: string; anomalies: string; tips: string } | null>(null);
  const [insightLoading, setInsightLoading] = useState(false);
  const [sliderIndex, setSliderIndex] = useState(0);

  useEffect(() => {
    fetchAvailableDays();
  }, []);

  const parseDate = (dateStr: string): Date => new Date(dateStr.replace(/ -\d{4}$/, ""));

  const fetchAvailableDays = async () => {
    try {
      const res = await fetch("/api/health-events/days");
      const days: string[] = await res.json();
      setAvailableDays(days);
      if (days.length > 0) {
        setSelectedDay(days[0]);
        await Promise.all(days.slice(0, 4).map((d) => fetchDayData(d)));
      }
    } catch (e) {
      console.error("Failed to fetch available days", e);
    } finally {
      setLoading(false);
    }
  };

  const fetchDayData = async (date: string) => {
    if (dayDataCache[date]) return;
    setDayLoading(true);
    try {
      const [hrRes, stepsRes, restingRes, energyRes, distanceRes, exerciseRes] = await Promise.all([
        fetch(`/api/health-events/heart_rate?date=${date}`),
        fetch(`/api/health-events/steps?date=${date}`),
        fetch(`/api/health-events/resting_heart_rate?date=${date}`),
        fetch(`/api/health-events/active_energy?date=${date}`),
        fetch(`/api/health-events/walking_running_distance?date=${date}`),
        fetch(`/api/health-events/exercise_time?date=${date}`),
      ]);
      const hr: HealthEvent[] = await hrRes.json();
      const steps: HealthEvent[] = await stepsRes.json();
      const resting: HealthEvent[] = await restingRes.json();
      const energy: HealthEvent[] = await energyRes.json();
      const distance: HealthEvent[] = await distanceRes.json();
      const exercise: HealthEvent[] = await exerciseRes.json();
      const summary = buildDaySummary(date, hr, steps, resting, energy, distance, exercise);
      setDayDataCache((prev) => ({ ...prev, [date]: { summary, hrEvents: hr } }));
    } catch (e) {
      console.error(`Failed to fetch data for ${date}`, e);
    } finally {
      setDayLoading(false);
    }
  };

  const buildDaySummary = (date: string, hr: HealthEvent[], steps: HealthEvent[], resting: HealthEvent[], energy: HealthEvent[], distance: HealthEvent[], exercise: HealthEvent[]): DaySummary => {
    const hrVals = hr.map((e) => Number(e.data?.Avg || e.data?.avg || 0)).filter((v) => v > 0);
    const totalSteps = steps.reduce((sum, e) => sum + Number(e.data?.qty || 0), 0);
    const restingHR = resting.length > 0 ? Number(resting[0].data?.qty || 0) : 0;
    const totalCalories = energy.reduce((sum, e) => sum + Number(e.data?.qty || 0), 0);
    const totalDistance = distance.reduce((sum, e) => sum + Number(e.data?.qty || 0), 0);
    const exerciseTime = exercise.reduce((sum, e) => sum + Number(e.data?.qty || 0), 0);
    return {
      date,
      avgHR: hrVals.length > 0 ? Math.round(hrVals.reduce((a, b) => a + b, 0) / hrVals.length) : 0,
      minHR: hrVals.length > 0 ? Math.min(...hrVals) : 0,
      maxHR: hrVals.length > 0 ? Math.max(...hrVals) : 0,
      restingHR,
      totalSteps: Math.round(totalSteps),
      totalCalories: Math.round(totalCalories),
      hrReadings: hrVals.length,
      totalDistance: Math.round(totalDistance * 100) / 100,
      exerciseTime: Math.round(exerciseTime),
    };
  };
  const getActiveMinutes = (): number => {
    if (!selectedDay || !dayDataCache[selectedDay]) return 0;
    return dayDataCache[selectedDay].hrEvents.filter((e) => {
      const avg = Number(e.data?.Avg || e.data?.avg || 0);
      return avg > 100;
    }).length;
  };

  const handleDaySelect = async (date: string) => {
    setSelectedDay(date);
    setInsight(null);
    await fetchDayData(date);
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

  const selectedSummary = selectedDay ? dayDataCache[selectedDay]?.summary ?? null : null;

  const visibleDays = availableDays.slice(sliderIndex, sliderIndex + 3);
  const canSlideLeft = sliderIndex > 0;
  const canSlideRight = sliderIndex + 3 < availableDays.length;

  const getHRChartData = () => {
    if (!selectedDay || !dayDataCache[selectedDay]) return null;
    const dayEvents = dayDataCache[selectedDay].hrEvents.slice().sort((a, b) => parseDate(a.date).getTime() - parseDate(b.date).getTime());
    return {
      labels: dayEvents.map((e) => parseDate(e.date).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })),
      datasets: [{
        label: "Heart Rate",
        data: dayEvents.map((e) => e.data?.Avg || e.data?.avg || 0),
        borderColor: "#ef4444",
        backgroundColor: "rgba(239, 68, 68, 0.1)",
        fill: true, tension: 0.4, pointRadius: 0, borderWidth: 2,
      }],
    };
  };

  const getLast4Summaries = () =>
      Object.values(dayDataCache).map((d) => d.summary).sort((a, b) => b.date.localeCompare(a.date)).slice(0, 4).reverse();

  const getStepsChartData = () => {
    const recent = getLast4Summaries();
    return {
      labels: recent.map((d) => new Date(d.date + "T00:00:00").toLocaleDateString([], { weekday: "short", month: "short", day: "numeric" })),
      datasets: [{ label: "Steps", data: recent.map((d) => d.totalSteps), backgroundColor: "rgba(34, 197, 94, 0.6)", borderColor: "#22c55e", borderWidth: 1, borderRadius: 6, barThickness: 14 }],
    };
  };

  const getCaloriesChartData = () => {
    const recent = getLast4Summaries();
    return {
      labels: recent.map((d) => new Date(d.date + "T00:00:00").toLocaleDateString([], { weekday: "short", month: "short", day: "numeric" })),
      datasets: [{ label: "Calories", data: recent.map((d) => d.totalCalories), backgroundColor: "rgba(251, 146, 60, 0.6)", borderColor: "#fb923c", borderWidth: 1, borderRadius: 6, barThickness: 14 }],
    };
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: { backgroundColor: "#1e293b", titleColor: "#f9fafb", bodyColor: "#94a3b8", borderColor: "rgba(255,255,255,0.1)", borderWidth: 1 },
    },
    scales: {
      x: { ticks: { color: "#64748b", maxTicksLimit: 8 }, grid: { color: "rgba(255,255,255,0.03)" } },
      y: { ticks: { color: "#64748b" }, grid: { color: "rgba(255,255,255,0.03)" } },
    },
  };

  if (loading) {
    return (
        <div className="homepage-container">
          <NavBar />
          <div className="main-content"><div className="health-loading">Loading health data...</div></div>
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

          {/* Day Slider */}
          <div className="day-slider">
            <button className={`slider-arrow ${!canSlideLeft ? "disabled" : ""}`} onClick={() => canSlideLeft && setSliderIndex(sliderIndex - 1)}>‹</button>
            <div className="day-slider-track">
              {visibleDays.map((date) => (
                  <div key={date} className={`day-pill ${selectedDay === date ? "active" : ""}`} onClick={() => handleDaySelect(date)}>
                    <span className="day-pill-label">{new Date(date + "T00:00:00").toLocaleDateString([], { weekday: "short" })}</span>
                    <span className="day-pill-date">{new Date(date + "T00:00:00").toLocaleDateString([], { month: "short", day: "numeric" })}</span>
                  </div>
              ))}
            </div>
            <button className={`slider-arrow ${!canSlideRight ? "disabled" : ""}`} onClick={() => canSlideRight && setSliderIndex(sliderIndex + 1)}>›</button>
          </div>

          {/* Metric Cards + AI Insights Row */}
          {dayLoading ? (
              <div className="health-loading">Loading day data...</div>
          ) : selectedSummary ? (
              <div className="metrics-insight-row">
                <div className="metric-cards-compact">
                  <div className="metric-card hr-card">
                    <div className="metric-card-header"><span className="metric-dot hr-dot"></span><span className="metric-label">Heart Rate</span></div>
                    <div className="metric-value">{selectedSummary.avgHR}</div>
                    <div className="metric-unit">avg bpm</div>
                    <div className="metric-range"><span>{selectedSummary.minHR} min</span><span>{selectedSummary.maxHR} max</span></div>
                  </div>
                  <div className="metric-card resting-card">
                    <div className="metric-card-header"><span className="metric-dot resting-dot"></span><span className="metric-label">Resting HR</span></div>
                    <div className="metric-value">{selectedSummary.restingHR || "—"}</div>
                    <div className="metric-unit">bpm</div>
                  </div>
                  <div className="metric-card steps-card">
                    <div className="metric-card-header"><span className="metric-dot steps-dot"></span><span className="metric-label">Steps</span></div>
                    <div className="metric-value">{selectedSummary.totalSteps.toLocaleString()}</div>
                    <div className="metric-unit">total</div>
                  </div>
                  <div className="metric-card calories-card">
                    <div className="metric-card-header"><span className="metric-dot calories-dot"></span><span className="metric-label">Active Energy</span></div>
                    <div className="metric-value">{selectedSummary.totalCalories.toLocaleString()}</div>
                    <div className="metric-unit">kcal</div>
                  </div>
                  <div className="metric-card distance-card">
                    <div className="metric-card-header"><span className="metric-dot distance-dot"></span><span className="metric-label">Distance</span></div>
                    <div className="metric-value">{selectedSummary.totalDistance.toLocaleString()}</div>
                    <div className="metric-unit">km</div>
                  </div>
                  <div className="metric-card exercise-card">
                    <div className="metric-card-header"><span className="metric-dot exercise-dot"></span><span className="metric-label">Exercise Time</span></div>
                    <div className="metric-value">{selectedSummary.exerciseTime}</div>
                    <div className="metric-unit">min</div>
                  </div>
                </div>

                <div className="insight-card">
                  <div className="insight-header">
                    <span className="insight-icon">✦</span>
                    <span className="insight-title">AI Health Insights</span>
                    {selectedDay && !insightLoading && !insight && (
                        <button className="insight-load-btn" onClick={() => fetchInsight(selectedDay)}>Load Insights</button>
                    )}
                  </div>
                  {insightLoading ? (
                      <div className="insight-loading"><div className="insight-loading-dot"></div><span>Analyzing your health data...</span></div>
                  ) : insight ? (
                      <div className="insight-body-vertical">
                        <div className="insight-section-v"><div className="insight-section-label">Summary</div><div className="insight-section-text">{insight.summary}</div></div>
                        <div className="insight-section-v"><div className="insight-section-label">Anomalies</div><div className="insight-section-text">{insight.anomalies}</div></div>
                        <div className="insight-section-v"><div className="insight-section-label">Tips</div><div className="insight-section-text">{insight.tips}</div></div>
                      </div>
                  ) : (
                      <div className="insight-idle">
                        <div className="insight-idle-rings">
                          <svg viewBox="0 0 100 100" className="insight-idle-svg">
                            <circle cx="50" cy="50" r="42" fill="none" stroke="rgba(139,92,246,0.08)" strokeWidth="4"/>
                            <circle cx="50" cy="50" r="42" fill="none" stroke="rgba(139,92,246,0.3)" strokeWidth="4" strokeDasharray="66 198" strokeLinecap="round" className="insight-ring-1"/>
                            <circle cx="50" cy="50" r="34" fill="none" stroke="rgba(99,102,241,0.08)" strokeWidth="3"/>
                            <circle cx="50" cy="50" r="34" fill="none" stroke="rgba(99,102,241,0.3)" strokeWidth="3" strokeDasharray="53 161" strokeLinecap="round" className="insight-ring-2"/>
                            <circle cx="50" cy="50" r="26" fill="none" stroke="rgba(167,139,250,0.08)" strokeWidth="2.5"/>
                            <circle cx="50" cy="50" r="26" fill="none" stroke="rgba(167,139,250,0.3)" strokeWidth="2.5" strokeDasharray="41 122" strokeLinecap="round" className="insight-ring-3"/>
                          </svg>
                          <div className="insight-idle-icon">✦</div>
                        </div>
                        <div className="insight-idle-text">Tap <strong>Load Insights</strong> to analyze</div>
                      </div>
                  )}
                </div>
              </div>
          ) : null}

          <div className="score-goals-row">
            <HealthScore selectedDay={selectedDay} />
            {selectedSummary && (
                <DailyGoals
                    steps={selectedSummary.totalSteps}
                    calories={selectedSummary.totalCalories}
                    hrReadings={selectedSummary.hrReadings}
                    activeMinutes={getActiveMinutes()}
                />
            )}
          </div>

          {/* Charts Row: HR timeline (half) + Steps & Energy stacked (half) */}
          <div className="charts-row">
            <div className="health-chart-box">
              <h3 className="chart-title">Heart Rate Timeline</h3>
              <div className="chart-container">
                {hrChart && hrChart.labels.length > 0 ? (
                    <Line data={hrChart} options={chartOptions} />
                ) : (
                    <div className="no-data">No heart rate data for this day</div>
                )}
              </div>
            </div>

            <div className="comparison-stack">
              <div className="health-chart-box">
                <h3 className="chart-title">Steps (Last 4 Days)</h3>
                <div className="chart-container">
                  {stepsChart && stepsChart.labels.length > 0 ? (
                      <Bar data={stepsChart} options={chartOptions} />
                  ) : (
                      <div className="no-data">No step data available</div>
                  )}
                </div>
              </div>
              <div className="health-chart-box">
                <h3 className="chart-title">Active Energy (Last 4 Days)</h3>
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
      </div>
  );
};

export default HealthDashboard;