import React , { useState , useEffect} from 'react'
import "./TopPanelStyle.css"

const TopPanel = () => {
    const [overallaverage,setOverallAverage] = useState<number>(0);
    const [todayavg,setTodayAvg] = useState<number>(0);
    const [rangaAvg,setRangaAvg] = useState<number>(0);

    useEffect(() =>{
        fetchOverallAvg();
        fetchRangeAvg();
        fetchTodayAvg();
      },[]);

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

    const fetchRangeAvg = async () => {
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
    return (
      <div className="Panel-container">
      <div className="stat-box blue">
        <div className="stat-text">
          <span className="stat-title">Total Orders</span>
          <span className="stat-value">{overallaverage}</span>
          <span className="stat-delta up">+2.3% from last week</span>
        </div>
        <div className="stat-icon">🛒</div>
      </div>

      <div className="stat-box red">
        <div className="stat-text">
          <span className="stat-title">Total Revenue</span>
          <span className="stat-value">{todayavg}</span>
          <span className="stat-delta up">+3.4% from last week</span>
        </div>
        <div className="stat-icon">💳</div>
      </div>

      <div className="stat-box green">
        <div className="stat-text">
          <span className="stat-title">Bounce Rate</span>
          <span className="stat-value">{todayavg}</span>
          <span className="stat-delta down">-4.5% from last week</span>
        </div>
        <div className="stat-icon">📊</div>
      </div>

      <div className="stat-box amber">
        <div className="stat-text">
          <span className="stat-title">Total Customers</span>
          <span className="stat-value">8.4K</span>
          <span className="stat-delta up">+8.4% from last week</span>
        </div>
        <div className="stat-icon">👤</div>
      </div>
    </div>
  );
};

export default TopPanel;

