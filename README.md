
# Glucose Monitoring Application
### Overview
A comprehensive full-stack web application for personal glucose monitoring and data visualization. This application integrates with Dexcom glucose monitoring device to provide real-time data visualization, statistical analysis, and historical tracking of glucose readings. Built with modern web technologies, it offers an intuitive interface for managing and analyzing blood glucose data to support better health management decisions.

### Architecture:
![dexcom (1)](https://github.com/user-attachments/assets/8317659a-3f3c-4880-8b83-cac47fe27f60)

Automated Collection: AWS EventBridge triggers Lambda every 5 minutes → Lambda fetches glucose readings from Dexcom API → stores in MongoDB Atlas.

User Access: React frontend requests data from Spring Boot API → Spring Boot queries MongoDB Atlas → returns formatted glucose data for Chart.js visualization.

#### Key Components:
##### 1. Data Ingestion Layer (AWS Serverless):
AWS EventBridge

Purpose: Orchestrates the automated data collection process
Configuration:

Scheduled rules (e.g., every 5 minutes for real-time glucose data)

Can be configured for different intervals based on Dexcom API rate limits

Handles timezone considerations for consistent data collection

AWS Lambda Function

Used Pydexcom library to get the details from Server.

Triggered every 5 minutes by AWS EventBridge.

##### 2.Data Storage Layer:

MongoDB Atlas

Used NoSQL model to store data.

Stored Glucose (Integer) and DateTime of reading.

##### 3.API Layer (Spring Boot):
RESTful API with layered architecture

Controller Layer → Service Layer → Repository Layer → MongoDB

Pagination: Handles large datasets efficiently

Filtering: Date ranges, glucose ranges, trend filtering

Aggregations: Statistical calculations (averages, std deviation)

##### 4.Frontend Layer (React TypeScript)
Initial Load: Fetch latest readings on component mount

User Interaction: Filter/sort triggers new API calls

State Management: React Context or Redux for global state

Visualization: Chart.js renders data with real-time updates

### Key Features

1. Real-time Data Visualization: Interactive charts using Chart.js for glucose trend analysis
2. Statistical Analysis: Calculate averages, standard deviation, and other metrics for glucose readings
3. Time-based Filtering: View glucose data by custom date ranges and time periods
4. Paginated Data Views: Efficiently handle large datasets with built-in pagination
5. MongoDB Integration: Secure cloud-based data storage using MongoDB Atlas
6. RESTful API: Well-structured backend API for data retrieval and management

### Technology Stack

#### Frontend
React 18+ with TypeScript for type-safe component development

Chart.js for data visualization

Lucide React for modern icon components

Vite for fast development and building

#### Backend

Spring Boot 3.2.0 for robust Java-based REST API

MongoDB for NoSQL database storage

MongoDB Atlas for cloud database hosting

Maven for dependency management


### Acknowledgments
Dexcom for glucose monitoring device integration

Chart.js community for visualization libraries

Spring Boot and React communities for excellent documentation

MongoDB Atlas for reliable cloud database services
