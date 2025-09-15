import React, { useState, useEffect } from "react";
import { Pie } from "react-chartjs-2";
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
} from "chart.js";

ChartJS.register(ArcElement, Tooltip, Legend);

interface GlucoseRangeCount {
  low: number;
  good: number;
  high: number;
  veryHigh: number;
}

const PieChart = () => {
  const [data, setData] = useState<GlucoseRangeCount | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const response = await fetch(`${import.meta.env.VITE_PieChart_Data}`);

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result: GlucoseRangeCount = await response.json();
        setData(result);
        setError(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : "An error occurred");
        console.error("Error fetching data:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) return <p>Loading chart...</p>;
  if (error) return <p>Error: {error}</p>;
  if (!data) return <p>No data available</p>;

  const chartData = {
    labels: ["Low (<120)", "Good (120–180)", "High (181–250)", "Very High (>250)"],
    datasets: [
      {
        label: "Glucose Range Count",
        data: [data.low, data.good, data.high, data.veryHigh],
        backgroundColor: [
          "#4ade80", 
          "#3b82f6", 
          "#facc15", 
          "#ef4444", 
        ],
        borderColor: "#fff",
        borderWidth: 2,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: "bottom" as const,
      },
    },
    cutout: "60%",
  };

  return (
    <div className="Piechart">
    <h2 className="text-xl font-semibold text-center mb-4">
      Glucose Levels Distribution
    </h2>
    <div style={{ width: "100%", height: "300px" }}>
      <Pie data={chartData} options={options} />
    </div>
  </div>
  );
};

export default PieChart;
