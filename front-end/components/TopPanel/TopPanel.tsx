import React , { useState , useEffect} from 'react'
import "./TopPanelStyle.css"
import { SlCalender } from "react-icons/sl";
import { FaClock } from "react-icons/fa6";

interface TopPanelProps {
  duration: number;
}

const TopPanel = ({duration}) => {
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
      <div className="stat-box blue">
        <div className="stat-text">
          <span className="stat-title">Overall Average</span>
          <span className="stat-value">{overallaverage}</span>
        </div>
        <div className="stat-icon"><SlCalender />        </div>
      </div>

      <div className="stat-box red">
        <div className="stat-text">
          <span className="stat-title">Today's Average</span>
          <span className="stat-value">{todayavg}</span>
        </div>
        <div className="stat-icon">💳</div>
      </div>

      <div className="stat-box green">
        <div className="stat-text">
          <span className="stat-title">Avg In Range selected</span>
          <span className="stat-value">{rangaAvg}</span>
          <span className="stat-delta down">Avg for Past {duration} hours</span>
        </div>
        <div className="stat-icon"><FaClock /></div>
      </div>

      <div className="stat-box amber">
        <div className="stat-text">
          <span className="stat-title">Customer Name</span>
          <span className="stat-value">Praveen Kumar</span>
          <span className="stat-delta up">Age: 25</span>
        </div>
        <div className="stat-icon">👤</div>
      </div>
    </div>
  );
};

export default TopPanel;

