import React, { useState } from "react";
import Graph from "./Graph/Graph";
import TopPanel from "./TopPanel/TopPanel";
import PieChart from "./PieChart/PieChart"
import "./HomePageStyle.css"

const HomePage: React.FC = () => {
  return (
    <>
      <div>
        <TopPanel />
        <div className="charts">
          <div className="line-box">
            <Graph />
          </div>
         <div className="pie-box">
            <PieChart />
          </div>
        </div>
        
      </div>
    </>
  );
};
export default HomePage;
