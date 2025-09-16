import React, { useEffect, useState } from "react";
import { Line} from "react-chartjs-2";
import.meta.env.graph_durationBased_data_endpoint;
import "../Graph/GraphStyle.css"
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
)

interface Glucose {
  DateTime: string;
  Glucose: number;
}

interface GraphProps {
  duration: number;
  setDuration: (value: number) => void;
}

const Graph = ({ duration , setDuration}) => {
  const[readings,setReadings] = useState<Glucose[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() =>{
    fetchReadings(duration);
  }, [duration]);

  const fetchReadings = async (hours:number) => {
    try{
      setError(null);
      const url = `${import.meta.env.VITE_graph_durationBased_data_endpoint}?hours=${hours}`;
      console.log("Fetching from:", url);
      const response = await fetch(url);
      if(!response.ok){
        throw new Error(`Failed to fetch readings: ${response.status}`);
      }

      const data: Glucose[] = await response.json();
      setReadings(data);
    }catch( error:any){
      console.log("Error fetching data",error);
      setError(error.message);
      setReadings([]);
    }
  };

  const handleDuration = (newDuration: number) => {
    setDuration(newDuration);
  };

  const chartData = {
    labels: readings.map((reading) =>
      new Date(reading.DateTime).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    ),
    datasets: [
      {
        label: "Glucose (mg/dL)",
        data: readings.map((reading) => reading.Glucose),
        borderColor: "rgba(75, 192, 192, 1)",
        backgroundColor: "rgba(75, 192, 192, 0.2)",
        tension: 0.4,
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top' as const,
      },
      title: {
        display: true,
        text: `Glucose Levels - Last ${duration} Hours`
      }
    },
    scales: {
      x: {
        display: true,
        title: {
          display: true,
          text: 'Date/Time'
        }
      },
      y: {
        display: true,
        title: {
          display: true,
          text: 'Glucose (mg/dL)'
        },
        suggestedMin: 70,
        suggestedMax: 250,
      }
    }
  };

  return (
    <div className="Graph-container">
      <div className="Range">
      <h3> Time Range</h3>
        <button className="Range-Buttons" onClick={() => handleDuration(3)}>
          3
        </button>
        <button className="Range-Buttons" onClick={() => handleDuration(6)}>
          6
        </button>
        <button className="Range-Buttons" onClick={() => handleDuration(12)}>
          12
        </button>
        <button className="Range-Buttons" onClick={() => handleDuration(24)}>
          24
        </button>
      </div>
      <div className="Graph">
        <div className="chart-Info">
          Showing {readings.length} readings from last {duration} hours
        </div>
        <Line data={chartData} options={chartOptions} />
      </div>
    </div>
  );


};
export default Graph;