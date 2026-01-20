/**
 * Logout user and clear all authentication data
 */
export const handleLogout = () => {
  console.log('🔴 Logout triggered');
  
  // Clear ALL possible localStorage keys
  localStorage.removeItem('librarian_token');
  localStorage.removeItem('librarian_user');
  localStorage.removeItem('temp_reset_token');
  
  // Clear ALL possible sessionStorage keys
  sessionStorage.removeItem('librarian_token');
  sessionStorage.removeItem('librarian_user');
  
  console.log('✅ All tokens cleared');
  
  // Redirect to login page
  window.location.href = '/';
};
