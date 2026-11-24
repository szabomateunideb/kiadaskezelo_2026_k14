import { useState } from 'react'
import heroImg from './assets/hero.png'
import reactLogo from './assets/react.svg'
import viteLogo from './assets/vite.svg'
import './App.css'
import HelloPage from "./pages/HelloPage.jsx";
import LoginPage from "./pages/LoginPage.jsx";

export default function App() {

  const [token, setToken] =
      useState(localStorage.getItem('token'))

  function belepes(ujToken){
    localStorage.setItem('token', ujToken)
    setToken(ujToken)
  }

  function kilepes(){
    localStorage.removeItem('token')
    setToken(null)
  }

  if( token ){
    return <HelloPage token={token} onKilepes={kilepes} />
  }
  return <LoginPage onSikeresBelepes={belepes} />
}
