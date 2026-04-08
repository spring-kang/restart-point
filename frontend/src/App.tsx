import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Layout from './components/layout/Layout';
import Home from './pages/Home';
import Login from './pages/Login';
import Signup from './pages/Signup';
import EmailVerification from './pages/EmailVerification';
import Certification from './pages/Certification';
import Profile from './pages/Profile';
import Seasons from './pages/Seasons';
import SeasonDetail from './pages/SeasonDetail';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5분
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<Home />} />
            <Route path="login" element={<Login />} />
            <Route path="signup" element={<Signup />} />
            <Route path="verify-email" element={<EmailVerification />} />
            <Route path="certification" element={<Certification />} />
            <Route path="profile" element={<Profile />} />
            <Route path="seasons" element={<Seasons />} />
            <Route path="seasons/:seasonId" element={<SeasonDetail />} />
            {/* 추후 추가될 라우트 */}
            {/* <Route path="seasons/:seasonId/teams" element={<Teams />} /> */}
            {/* <Route path="teams/:id" element={<TeamDetail />} /> */}
            {/* <Route path="my-team" element={<MyTeam />} /> */}
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
