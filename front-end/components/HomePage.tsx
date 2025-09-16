import React, { useState } from "react";
import Graph from "./Graph/Graph";
import TopPanel from "./TopPanel/TopPanel";
import PieChart from "./PieChart/PieChart"
import "./HomePageStyle.css"

const HomePage: React.FC = () => {
  const [duration, setDuration] = useState<number>(24);

  return (
    <>
      <div>
        <TopPanel duration={duration} />
        <div className="charts">
          <div className="line-box">
            <Graph duration={duration} setDuration={setDuration}/>
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
