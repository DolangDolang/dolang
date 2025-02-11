import { useNavigate } from 'react-router';
import Cookies from 'js-cookie';

const accessToken = Cookies.get('access_token');

interface ImportMetaEnv {
  readonly VITE_USER_REDIRECT_URI: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

const REDIRECT_URI = import.meta.env.VITE_USER_REDIRECT_URI;
const token = accessToken;

export const userPut = async (data: string, navigate: ReturnType<typeof useNavigate>) => {
  try {
    const response = await fetch(`${REDIRECT_URI}/api/user`, {
      method: 'GET',
      headers: {
        accept: '*/*',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(data),
    });

    if (response.ok) {
      console.log(response);
    }
  } catch (error) {
    console.log(error);
  }
};
