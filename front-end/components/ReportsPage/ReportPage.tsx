import React, { useState, useEffect } from "react";
import Navbar from "../Navbar/NavBar"; 
import "./ReportPageStyle.css";

const PaginatedTable: React.FC = () => {
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [data, setData] = useState<any[]>([]);
  const [totalPages, setTotalPages] = useState(0);

  const fetchData = async () => {
    if (!startDate || !endDate) return;
    const baseUrl = import.meta.env.VITE_getPaginatedData;
    const url = `${baseUrl}?startDate=${startDate}T00:00:00&endDate=${endDate}T23:59:59&page=${page}&size=${size}`;
    const res = await fetch(url);
    const result = await res.json();

    setData(result.content || []);
    setTotalPages(result.totalPages || 0);
  };

  useEffect(() => {
    if (startDate && endDate) fetchData();
  }, [page, size]);

  const downloadPdf = async () => {
    if (!startDate || !endDate) return alert("Please select a date range");
  
    const baseUrl = import.meta.env.VITE_getPdfData;
    const url = `${baseUrl}?startDate=${startDate}T00:00:00&endDate=${endDate}T23:59:59`;
  
    try {
      const res = await fetch(url, {
        method: "GET",
        headers: {
          Accept: "application/pdf",
        },
      });
  
      if (!res.ok) {
        throw new Error("Failed to download PDF");
      }
  
      const blob = await res.blob();
      const link = document.createElement("a");
      link.href = URL.createObjectURL(blob);
      link.download = "glucose_report.pdf";
      link.click();
      URL.revokeObjectURL(link.href);
    } catch (err) {
      console.error(err);
      alert("Error downloading PDF");
    }
  };

  return (
    <div className="report-layout">
      <Navbar />
      <div className="report-content">
        <h2>Glucose Readings (Paginated)</h2>

        <div className="filters">
          <label>
            Start Date:
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />
          </label>
          <label>
            End Date:
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
            />
          </label>
          <button onClick={fetchData}>Fetch Data</button>

          <button onClick={downloadPdf}>Download PDF</button>
        </div>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>DateTime</th>
                <th>Glucose</th>
              </tr>
            </thead>
            <tbody>
              {data.length > 0 ? (
                data.map((row, idx) => (
                  <tr key={idx}>
                    <td>{new Date(row.DateTime).toLocaleString()}</td>
                    <td>{row.Glucose}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={2}>No Data</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="pagination">
          <button
            onClick={() => setPage(Math.max(page - 1, 0))}
            disabled={page === 0}
          >
            Prev
          </button>
          <span>
            Page {page + 1} {totalPages > 0 && `of ${totalPages}`}
          </span>
          <button
            onClick={() => setPage(page + 1)}
            disabled={page + 1 >= totalPages}
          >
            Next
          </button>
          <select value={size} onChange={(e) => setSize(Number(e.target.value))}>
            <option value={5}>5 rows</option>
            <option value={10}>10 rows</option>
            <option value={20}>20 rows</option>
          </select>
        </div>
      </div>
    </div>
  );
};

export default PaginatedTable;
