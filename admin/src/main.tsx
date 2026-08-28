import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import './styles.css';

ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><ConfigProvider locale={zhCN} theme={{ token: { colorPrimary: '#28604f', colorSuccess: '#3d7865', colorError: '#b94f3c', colorText: '#17362d', colorTextSecondary: '#7b8e87', colorBgLayout: '#f6f7f4', colorBorder: '#dce6e1', borderRadius: 12, fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif" }, components: { Layout: { siderBg: '#173f34', headerBg: '#ffffff' }, Menu: { darkItemBg: '#173f34', darkItemSelectedBg: '#e4bd6d', darkItemSelectedColor: '#173f34', darkItemHoverBg: '#245d4d' }, Button: { primaryShadow: '0 10px 20px rgba(36,88,72,.18)' }, Table: { headerBg: '#f3f6f4', headerColor: '#28463c' } } }}><BrowserRouter><App /></BrowserRouter></ConfigProvider></React.StrictMode>);
