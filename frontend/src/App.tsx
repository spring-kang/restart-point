import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Layout from './components/layout/Layout';
import Home from './pages/Home';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Certification from './pages/Certification';
import Profile from './pages/Profile';
import Seasons from './pages/Seasons';
import SeasonDetail from './pages/SeasonDetail';
import Teams from './pages/Teams';
import TeamDetail from './pages/TeamDetail';
import MyTeam from './pages/MyTeam';
import ProjectWorkspace from './pages/ProjectWorkspace';
import ReviewProjects from './pages/ReviewProjects';
import GrowthReport from './pages/GrowthReport';
import FeaturedProjects from './pages/FeaturedProjects';
import Community from './pages/Community';
import PostDetail from './pages/PostDetail';
import PostWrite from './pages/PostWrite';
import Notifications from './pages/Notifications';

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
            <Route path="certification" element={<Certification />} />
            <Route path="profile" element={<Profile />} />
            <Route path="seasons" element={<Seasons />} />
            <Route path="seasons/:seasonId" element={<SeasonDetail />} />
            <Route path="seasons/:seasonId/teams" element={<Teams />} />
            <Route path="teams" element={<Teams />} />
            <Route path="teams/:teamId" element={<TeamDetail />} />
            <Route path="teams/:teamId/project" element={<ProjectWorkspace />} />
            <Route path="my-team" element={<MyTeam />} />
            <Route path="seasons/:seasonId/review" element={<ReviewProjects />} />
            <Route path="projects/:projectId/growth-report" element={<GrowthReport />} />
            <Route path="featured-projects" element={<FeaturedProjects />} />
            <Route path="community" element={<Community />} />
            <Route path="community/posts/:postId" element={<PostDetail />} />
            <Route path="community/write" element={<PostWrite />} />
            <Route path="notifications" element={<Notifications />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
