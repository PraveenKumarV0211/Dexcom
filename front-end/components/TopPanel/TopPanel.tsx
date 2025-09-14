import React , { useState , useEffect} from 'react'
import "./TopPanelStyle.css"

const TopPanel = () => {
    const [overallaverage,setOverallAverage] = useState<number>(0);
    const [todayavg,setTodayAvg] = useState<number>(0);
    const [rangaAvg,setRangaAvg] = useState<number>(0);

    useEffect(() =>{
        fetchOverallAvg();
      },[]);

      useEffect(() =>{
        fetchTodayAvg();
      },[]);

      useEffect(() =>{
        fetchRangeAvg();
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
            const response = await fetch(`${import.meta.env.VITE_Overall_Avg}`);
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
            const response = await fetch(`${import.meta.env.VITE_Overall_Avg}`);
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
        <div className='Panel-container'>
            <div className='box'>
                {overallaverage}
            </div>
            <div className='box'>
                {todayavg}
            </div>
            <div className='box'>
                {rangaAvg}
            </div>
        </div>
    );
};

export default TopPanel;

