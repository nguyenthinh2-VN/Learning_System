import { useState } from 'react';
import InternalLoginForm from './InternalLoginForm';
import DashboardContent from './DashboardContent';

const INTERNAL_USER_KEY = 'internalUser';
const INTERNAL_TOKEN_KEY = 'internalToken';

function getStoredInternalUser() {
  try {
    const token = localStorage.getItem(INTERNAL_TOKEN_KEY);
    const user = localStorage.getItem(INTERNAL_USER_KEY);
    if (token && user) return JSON.parse(user);
    return null;
  } catch {
    return null;
  }
}

export default function DashboardGate() {
  const [internalUser, setInternalUser] = useState(() => getStoredInternalUser());

  const handleLoginSuccess = (user) => {
    setInternalUser(user);
  };

  const handleLogout = () => {
    localStorage.removeItem(INTERNAL_TOKEN_KEY);
    localStorage.removeItem(INTERNAL_USER_KEY);
    setInternalUser(null);
  };

  if (internalUser) {
    return <DashboardContent user={internalUser} onLogout={handleLogout} />;
  }

  return <InternalLoginForm onSuccess={handleLoginSuccess} />;
}
