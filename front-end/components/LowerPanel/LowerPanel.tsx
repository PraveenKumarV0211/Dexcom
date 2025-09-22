import React, { use, useEffect, useState } from "react";
import "./LowerPanel.css";
import { Info, Calendar } from 'lucide-react';

const LowerPanel: React.FC = () => {
const[gmi,setGmi] = useState<number>(0.0);
const[standardDeviation,setStandardDeviation] = useState<number>(0.0);
const [glucoseValue, setGlucoseValue] = useState<number | "">("");
  const [dateTime, setDateTime] = useState<string>("");
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
    const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      if (glucoseValue === "" || !dateTime) {
        alert("Please provide both glucose value and date-time.");
        return;
      }
    
      try {
        const localDate = new Date(dateTime);
        const isoWithOffset = localDate.toISOString();
    
        const params = new URLSearchParams({
          glucoseValue: glucoseValue.toString(),
          time: isoWithOffset,
        });
    
        const response = await fetch(
          `${import.meta.env.VITE_Add_Glucose_Reading}?${params.toString()}`,
          { method: "POST" }
        );
    
        if (response.ok) {
          alert("Glucose reading added successfully!");
          setGlucoseValue("");
          setDateTime("");
        } else {
          alert("Failed to add glucose reading.");
        }
      } catch (err) {
        console.error(err);
        alert("Error occurred while sending data.");
      }
    };
    

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
      <span className="info-title">Manual Glucose Insertion</span>
      <div className="tooltip-container">
        <Info className="tooltip-icon" />
        <span className="tooltip-text">
        </span>
      </div>
    </div>
    <form onSubmit={handleSubmit}>
      <input 
        type="number"
        placeholder="Glucose Value"
        value={glucoseValue}
        onChange={(e) =>
          setGlucoseValue(
            e.target.value === "" ? "" : parseInt(e.target.value, 10)
          )
        }
        className="info-input"
        required
      />
      
      <div className="datetime-wrapper">
        <input
          type="datetime-local"
          id="datetime-input"
          value={dateTime}
          onChange={(e) => setDateTime(e.target.value)}
          className="hidden-datetime-input"
          required
        />
        <button 
          type="button"
          className="calendar-trigger"
          onClick={() => document.getElementById('datetime-input').showPicker()}
        >
          <Calendar className="calendar-icon" />
          <span className="date-display">
            
          </span>
        </button>
      </div>
      
      <button type="submit" className="info-submit">
        Submit
      </button>
    </form>
  </div>
</div>

    </div>
  );
};

export default LowerPanel;
