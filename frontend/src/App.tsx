import { Routes, Route } from 'react-router-dom';
import ExploraMapViewer from './pages/ExploraMapViewer';
import Admin from './pages/Admin';

function App() {
  return (
      <Routes>
          <Route path="/" element={<ExploraMapViewer />} />
          <Route path="/admin" element={<Admin />} />
      </Routes>
  )}

export default App
