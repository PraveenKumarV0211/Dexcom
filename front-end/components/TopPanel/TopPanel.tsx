import React , { useState , useEffect} from 'react'
import "./TopPanelStyle.css"
import { SlCalender } from "react-icons/sl";
import { FaClock } from "react-icons/fa6";

interface TopPanelProps {
  duration: number;
}

const getGlucoseColor = (value: number): string => {
  if (value < 120) return "#4ade80";    // green - low/normal
  if (value <= 180) return "#3b82f6";   // blue - good
  if (value <= 250) return "#f59e0b";   // amber - high
  return "#ef4444";                      // red - very high
};

const getGlucoseLabel = (value: number): string => {
  if (value < 120) return "Low";
  if (value <= 180) return "In Range";
  if (value <= 250) return "High";
  return "Very High";
};

const TopPanel = ({duration}: TopPanelProps) => {
    const [overallaverage,setOverallAverage] = useState<number>(0);
    const [todayavg,setTodayAvg] = useState<number>(0);
    const [rangaAvg,setRangaAvg] = useState<number>(0);

    useEffect(() =>{
        fetchOverallAvg();
        fetchRangeAvg(duration);
        fetchTodayAvg();
      },[duration]);

      const fetchOverallAvg = async () => {
        try{
            const response = await fetch(`${import.meta.env.VITE_Overall_Avg}`);
            if(!response.ok){
              throw new Error(`Failed to fetch readings: ${response.status}`);
            }
            const data = await response.json();
            if( typeof data  === 'number'){
                setOverallAverage(data);
            }
      }catch (error){

      };
    }

    const fetchTodayAvg = async () => {
        try{
            const response = await fetch(`${import.meta.env.VITE_CurrentDay_Avg}`);
            if(!response.ok){
              throw new Error(`Failed to fetch readings: ${response.status}`);
            }
            const data = await response.json();
            if( typeof data  === 'number'){
                setTodayAvg(data);
            }
      }catch (error){

      };
    }

    const fetchRangeAvg = async (duration:number) => {
        try{
            const response = await fetch(`${import.meta.env.VITE_Range_Avg}?hours=${duration}`);
            if(!response.ok){
              throw new Error(`Failed to fetch readings: ${response.status}`);
            }
            const data = await response.json();
            if( typeof data  === 'number'){
                setRangaAvg(data);
            }
      }catch (error){

      };
    }


    
    return (
      <div className="Panel-container">
      <div className="stat-box" style={{ borderLeftColor: getGlucoseColor(overallaverage) }}>
        <div className="stat-text">
          <span className="stat-title">Overall Average</span>
          <span className="stat-value" style={{ color: getGlucoseColor(overallaverage) }}>{overallaverage}</span>
          <span className="stat-label" style={{ color: getGlucoseColor(overallaverage) }}>{getGlucoseLabel(overallaverage)}</span>
        </div>
        <div className="stat-icon" style={{ backgroundColor: getGlucoseColor(overallaverage) }}><SlCalender /></div>
      </div>

      <div className="stat-box" style={{ borderLeftColor: getGlucoseColor(todayavg) }}>
        <div className="stat-text">
          <span className="stat-title">Today's Average</span>
          <span className="stat-value" style={{ color: getGlucoseColor(todayavg) }}>{todayavg}</span>
          <span className="stat-label" style={{ color: getGlucoseColor(todayavg) }}>{getGlucoseLabel(todayavg)}</span>
        </div>
        <div className="stat-icon" style={{ backgroundColor: getGlucoseColor(todayavg) }}>💳</div>
      </div>

      <div className="stat-box" style={{ borderLeftColor: getGlucoseColor(rangaAvg) }}>
        <div className="stat-text">
          <span className="stat-title">Avg In Range selected</span>
          <span className="stat-value" style={{ color: getGlucoseColor(rangaAvg) }}>{rangaAvg}</span>
          <span className="stat-label" style={{ color: getGlucoseColor(rangaAvg) }}>{getGlucoseLabel(rangaAvg)}</span>
          <span className="stat-delta">Avg for Past {duration} hours</span>
        </div>
        <div className="stat-icon" style={{ backgroundColor: getGlucoseColor(rangaAvg) }}><FaClock /></div>
      </div>

      <div className="stat-box" style={{ borderLeftColor: "#f59e0b" }}>
        <div className="stat-text">
          <span className="stat-title">Customer Name</span>
          <span className="stat-value" style={{ color: "#f9fafb" }}>Praveen Kumar</span>
          <span className="stat-delta up">Age: 25</span>
        </div>
        <div className="stat-icon" style={{ backgroundColor: "#f59e0b" }}>👤</div>
      </div>
    </div>
  );
};

export default TopPanel;