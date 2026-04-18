# FaceTrackU - AI-Powered Face Attendance System

## 🎓 Project Overview

FaceTrackU is an advanced attendance management system that uses facial recognition technology to automate student attendance tracking. Built with modern web technologies and AI/ML, it provides real-time attendance monitoring, comprehensive reporting, and an intuitive user interface.

## ✨ Key Features

### 🎯 Core Features
- **AI-Powered Face Recognition** - 90%+ accuracy with confidence scoring
- **Real-Time Attendance** - Live feed with auto-refresh every 10 seconds
- **Multi-Angle Face Enrollment** - Capture multiple poses for better accuracy
- **Comprehensive Reports** - Analytics, calendar view, and detailed records
- **Export Functionality** - CSV, Excel, and PDF export with filters
- **Subject-Based Filtering** - Dynamic subject dropdown from database

### 📊 Dashboard Features
- Live attendance feed with real-time updates
- Department-wise statistics
- Attendance trend visualization
- Quick action buttons
- Performance metrics

### 👤 Student Management
- Complete student profiles with attendance history
- Clickable student names for detailed view
- Face enrollment status tracking
- Attendance percentage calculation
- Streak tracking for consecutive attendance

### 🎨 UI/UX Features
- **4 Theme Options** - Light, Dark, Blue, Green
- **Liquid Glass Notifications** - Beautiful glassmorphism with 2-sec duration
- **Responsive Design** - Works on desktop, tablet, and mobile
- **Smooth Animations** - Framer Motion powered transitions
- **Dark Mode Support** - Seamless theme switching

## 🛠️ Technology Stack

### Frontend
- **React 18** - UI framework
- **TypeScript** - Type safety
- **Tailwind CSS** - Styling
- **Framer Motion** - Animations
- **React Router** - Navigation
- **Recharts** - Data visualization
- **React Hot Toast** - Notifications
- **Lucide React** - Icons

### Backend
- **Spring Boot 3** - Java framework
- **Spring Security** - Authentication & authorization
- **JPA/Hibernate** - ORM
- **PostgreSQL** - Database (Supabase hosted)
- **Maven** - Build tool
- **RESTful APIs** - Backend services

### AI/ML
- **Face Recognition** - DeepFace / face-api.js
- **Confidence Scoring** - Match percentage calculation
- **Multi-angle capture** - Enhanced recognition accuracy

## 📁 Project Structure

```
FaceTrackU-Attendance-System/
├── frontend/                 # React TypeScript application
│   ├── src/
│   │   ├── components/      # Reusable components
│   │   ├── pages/           # Page components
│   │   ├── contexts/        # React contexts
│   │   ├── config/          # Configuration files
│   │   └── App.tsx          # Main app component
│   ├── public/              # Static assets
│   └── package.json         # Dependencies
│
├── backend/                  # Spring Boot application
│   ├── src/main/java/
│   │   └── com/faceattendance/
│   │       ├── controller/  # REST controllers
│   │       ├── service/     # Business logic
│   │       ├── model/       # Entity models
│   │       └── repository/  # Data access layer
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml              # Maven dependencies
│
├── docs/                     # Documentation
├── screenshots/              # UI screenshots
└── README.md                 # This file
```

## 🚀 Getting Started

### Prerequisites
- **Node.js** 16+ and npm
- **Java** 17+
- **Maven** 3.8+
- **PostgreSQL** database (or Supabase account)

### Backend Setup

1. Navigate to backend directory:
```bash
cd backend
```

2. Configure database in `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://your-db-host:5432/postgres
spring.datasource.username=your-username
spring.datasource.password=your-password
```

3. Run the Spring Boot application:
```bash
mvn spring-boot:run
```

Backend will start on `http://localhost:8080`

### Frontend Setup

1. Navigate to frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm start
```

Frontend will open on `http://localhost:3000`

## 📡 API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout

### Students
- `GET /api/students` - Get all students
- `GET /api/students/{id}` - Get student by ID
- `POST /api/students` - Add new student
- `PUT /api/students/{id}` - Update student
- `DELETE /api/students/{id}` - Delete student

### Attendance
- `GET /api/attendance` - Get all attendance records
- `GET /api/attendance/subjects` - Get unique subjects
- `GET /api/attendance/stats/today` - Today's statistics
- `POST /api/attendance/mark` - Mark attendance

### Reports
- `GET /api/reports/department` - Department-wise reports
- `GET /api/reports/student/{id}` - Student attendance report

## 🎨 Features in Detail

### 1. Face Recognition
- Multi-angle face capture during enrollment
- Real-time recognition with confidence scoring
- Minimum 90% threshold for attendance marking
- Support for multiple lighting conditions

### 2. Reports System
- **Analytics Dashboard** - Overview with charts
- **Calendar View** - Monthly attendance heatmap
- **Detailed Records** - Filterable attendance list
- **Export Options** - CSV, Excel, PDF

### 3. Student Profile Modal
- Complete attendance history
- 7-week trend visualization
- Streak tracking
- Face enrollment status
- Contact information

### 4. Live Attendance Feed
- Auto-refreshes every 10 seconds
- Shows today's attendance only
- Color-coded confidence scores
- Clickable student names
- Real-time updates

### 5. Theme System
- Light theme (default)
- Dark theme (professional)
- Blue theme (cool)
- Green theme (fresh)
- Smooth transitions between themes
- Persistent preference storage

## 🔐 Security Features

- JWT-based authentication
- Role-based access control (RBAC)
- Password encryption
- CORS configuration
- Secure API endpoints
- Session management

## 📊 Database Schema

### Students Table
- id (Primary Key)
- firstName
- lastName
- ienNumber (Unique)
- email
- phone
- department
- branch
- year
- section
- faceDescriptor (Base64)

### Attendance Table
- id (Primary Key)
- studentId (Foreign Key)
- timestamp
- confidence
- method
- subject

### Users Table
- id (Primary Key)
- username
- password (Encrypted)
- role
- firstName
- lastName

## 🎯 Future Enhancements

- [ ] Advanced search and filters
- [ ] Quick stats dashboard widgets
- [ ] Attendance history timeline
- [ ] Bulk operations
- [ ] Email notifications
- [ ] SMS alerts for low attendance
- [ ] Parent portal
- [ ] Mobile app (React Native)
- [ ] Biometric integration
- [ ] Cloud face storage
- [ ] Analytics AI insights

## 📝 Version History

### Version 1.0.0 (Current)
- Initial release
- Core face recognition
- Basic reporting
- Student management
- Authentication system

### Recent Updates (Oct 26, 2025)
- ✅ Added subject filter with real backend data
- ✅ Implemented calendar view in reports
- ✅ Added export functionality (CSV/Excel/PDF)
- ✅ Created liquid glass notifications
- ✅ Enhanced dark mode with 4 themes
- ✅ Built live attendance feed
- ✅ Added student profile modal

## 👥 Team / Author

- **Sneha Sahu** - Full Stack Development

## 📄 License

This project is for educational purposes.

## 🙏 Acknowledgments

- React team for the amazing framework
- Spring Boot community
- Face recognition library contributors
- All open-source contributors

## 📞 Support

For issues and questions:
- Open an issue in the repository
- Contact: [Your email]

## 🌟 Screenshots

(Add your screenshots in the /screenshots folder)

---

**Built with ❤️ using React, Spring Boot, and AI/ML**
