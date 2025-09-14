import React, { useState } from "react";
import Graph from "./Graph/Graph";
import TopPanel from "./TopPanel/TopPanel";

const HomePage: React.FC = () => {
  return (
    <>
      <div>
        <TopPanel />
        <Graph />
      </div>
    </>
  );
};
export default HomePage;
