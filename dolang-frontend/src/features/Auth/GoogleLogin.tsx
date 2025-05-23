import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { useRecoilState } from 'recoil';
import { authState } from './authState.ts';
import { useMutation, useQueryClient } from 'react-query';
import { userGet } from '../../api/utils/useUser.ts';
import GoogleAuthModal from './GoogleAuthModal.tsx';
import Cookies from 'js-cookie';

interface ImportMetaEnv {
  readonly VITE_GOOGLE_CLIENT_ID: string;
  readonly VITE_GOOGLE_REDIRECT_URI: string;
  readonly VITE_GOOGLE_AUTH_SERVER_URL: string;
  readonly VITE_GOOGLE_AUTHORIZATION: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;
const REDIRECT_URI = import.meta.env.VITE_GOOGLE_REDIRECT_URI;
const AUTH_SERVER_URL = import.meta.env.VITE_GOOGLE_AUTH_SERVER_URL;
const AUTHORIZATION = import.meta.env.VITE_GOOGLE_AUTHORIZATION;

const GoogleLogin = () => {
  const navigate = useNavigate();
  const [isAuthenticating, setIsAuthenticating] = useState(false);
  const [accessToken, setAccessToken] = useState<string | undefined>(undefined);
  const [auth, setAuth] = useRecoilState(authState);
  // const queryClient = useQueryClient();

  useEffect(() => {
    sessionStorage.setItem(
      'user',
      JSON.stringify({ ...auth.user, profileImageUrl: auth.user?.profileImageUrl || '/default-user.png' })
    );
  }, [auth]);

  // 1. Authorization code 발급
  const handleGoogleLogin = () => {
    const googleAuthUrl = `${AUTH_SERVER_URL}/oauth2/authorize?
    response_type=code
    &client_id=${CLIENT_ID}
    &redirect_uri=${REDIRECT_URI}
    &scope=openid`.replace(/\s+/g, '');

    window.location.href = googleAuthUrl;
  };

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');

    if (code) {
      codeForToken(code);
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, []);

  // 2. Authorization code로 Token 발급
  const codeForToken = async (code: string) => {
    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      code: code,
      redirect_uri: REDIRECT_URI,
    });

    try {
      const response = await fetch(`${AUTH_SERVER_URL}/oauth2/token`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          Authorization: `${AUTHORIZATION}`,
        },
        body: body.toString(),
        credentials: 'include', //refresh token 쿠키로 저장
      });
      if (!response.ok) {
        throw new Error(`Token request failed: ${response.status} ${response.statusText}`);
      }

      const data = await response.json();

      if (data.access_token && data.refresh_token) {
        console.log(data.access_token);
        saveAccessTokenToCookie(data.access_token);
        saveRefreshTokenToCookie(data.refresh_token);
        setAccessToken(data.access_token);

        Cookies.set('access_token', data.access_token);
        Cookies.set('refresh_token', data.refresh_token);

        setAuth((prevAuth) => ({
          ...prevAuth,
        }));

        const res = await userGet(data.access_token);
        if (res.result?.nickname && res.result?.nationality) {
          alert('로그인 되었습니다.');
          sessionStorage.setItem('isLoggedIn', JSON.stringify(true));
          navigate('/');
          setAuth({
            isLoggedIn: true,
            accessToken: data.access_token,
            refreshToken: data.refresh_token,
            user: res.result,
          });
        } else {
          setAuth({
            isLoggedIn: true,
            accessToken: data.access_token,
            refreshToken: data.refresh_token,
            user: null,
          });
          navigate('/signup/register');
        }
      }
    } catch (err) {
      console.error('Error code for Token: ', err);
    }
  };

  // 3-1. [Token 세션 쿠키 저장] Access Token
  const saveAccessTokenToCookie = (token: string) => {
    document.cookie = `access_token=${token}; path=/; SameSite=None;`;
  };

  // 3-2. [Token 세션 쿠키 저장] Refresh Token
  const saveRefreshTokenToCookie = (token: string) => {
    document.cookie = `refresh_token=${token}; path=/; SameSite=None;`;
  };

  return (
    <>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          height: '80vh',
        }}
      >
        <div
          style={{
            width: '400px',
            height: '450px',
            border: '1px solid #eee',
            borderRadius: '8px',
            boxShadow: '0 4px 4px rgba(0, 0, 0, 0.2)',
          }}
        >
          <GoogleAuthModal onLoginClick={handleGoogleLogin} />
        </div>
      </div>
      {/* <LogInModal onLoginClick={handleGoogleLogin} /> */}
    </>
  );
};
export default GoogleLogin;
