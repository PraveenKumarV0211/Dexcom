import React, { use, useEffect, useState } from "react";
import "./LowerPanel.css";
import { Info } from "lucide-react"; 

const LowerPanel: React.FC = () => {
const[gmi,setGmi] = useState<number>(0.0);
const[standardDeviation,setStandardDeviation] = useState<number>(0.0);
useEffect(() =>{
fetchGMI();
fetchStandardDeviation();
},[])
    const fetchGMI = async () => {
        try{
            const response = await fetch(`${import.meta.env.VITE_Overall_Avg}`);
            if(!response.ok){
              throw new Error(`Failed to fetch readings: ${response.status}`);
            }
            const data = await response.json();
            if( typeof data  === 'number'){
                setGmi(data);
            }
      }catch (error){

      };
    }

    const fetchStandardDeviation = async () => {
        try{
            const response = await fetch(`${import.meta.env.VITE_Get_StandardDeviation}`);
            if(!response.ok){
              throw new Error(`Failed to fetch readings: ${response.status}`);
            }
            const data = await response.json();
            if( typeof data  === 'number'){
                setStandardDeviation(data);
            }
      }catch (error){

      };
    }

  return (
    <div className="info-panel">
      <div className="info-box">
        <div className="info-text">
          <div className="info-header">
            <span className="info-title">Overall Visitors</span>
            <div className="tooltip-container">
              <Info className="tooltip-icon" />
              <span className="tooltip-text">
                Glucose Management Indicator
              </span>
            </div>
          </div>
          <span className="info-value">{gmi}</span>
        </div>
      </div>

      <div className="info-box">
        <div className="info-text">
          <div className="info-header">
            <span className="info-title">Standard Deviation</span>
            <div className="tooltip-container">
              <Info className="tooltip-icon" />
              <span className="tooltip-text">
                Average time users spend during their visit.
              </span>
            </div>
          </div>
          <span className="info-value">{standardDeviation}</span>
          <span className="info-delta up">+12.65%</span>
        </div>
      </div>

      <div className="info-box">
        <div className="info-text">
          <div className="info-header">
            <span className="info-title">Pages per Visit</span>
            <div className="tooltip-container">
              <Info className="tooltip-icon" />
              <span className="tooltip-text">
                Average number of pages viewed per visit.
              </span>
            </div>
          </div>
          <span className="info-value">639.82</span>
          <span className="info-delta up">+5.62%</span>
        </div>
      </div>

    </div>
  );
};

export default LowerPanel;
