import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import StudentsManage from './components/students/StudentsManage';
import StudentDetail from './components/students/StudentDetail';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/students" />} />
        <Route path="/students" element={<StudentsManage />} />
        <Route path="/students/:studentId" element={<StudentDetail />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;