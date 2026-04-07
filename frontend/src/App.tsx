import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Layout from './components/layout/Layout';
import Home from './pages/Home';

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
            {/* 추후 추가될 라우트 */}
            {/* <Route path="login" element={<Login />} /> */}
            {/* <Route path="signup" element={<Signup />} /> */}
            {/* <Route path="profile" element={<Profile />} /> */}
            {/* <Route path="certification" element={<Certification />} /> */}
            {/* <Route path="seasons" element={<Seasons />} /> */}
            {/* <Route path="seasons/:id" element={<SeasonDetail />} /> */}
            {/* <Route path="teams" element={<Teams />} /> */}
            {/* <Route path="teams/:id" element={<TeamDetail />} /> */}
            {/* <Route path="my-team" element={<MyTeam />} /> */}
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
