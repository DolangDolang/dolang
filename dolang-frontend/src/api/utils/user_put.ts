import axios from 'axios';
import { useNavigate } from 'react-router';
import Cookies from 'js-cookie';

const accessToken = Cookies.get('access_token');

type Interest = {
  tagId: number;
  // nativeLanguageId: string;
  name: string;
};

interface PutData {
  nickname: string;
  nationality: string;
  nativeLanguage: string;
  targetLanguage: string;
  proficiencyLevel: string;
  interests: Interest[];
  profileImageUrl?: string;
}
interface ImportMetaEnv {
  readonly VITE_USER_SERVER_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

const SERVER_URL = import.meta.env.VITE_USER_SERVER_URL;
const token = accessToken;

export const userPut = async (data: PutData, access_token?: string) => {
  const token = access_token || accessToken;
  try {
    const response = await fetch(`${SERVER_URL}/api/user`, {
      method: 'PUT',
      headers: {
        accept: '*/*',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(data),
    });

    if (response.status === 401) {
      alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
      Cookies.remove('access_token'); // 토큰 삭제
      sessionStorage.removeItem('user'); // 세션 데이터 삭제
      window.location.href = '/login'; // 로그인 페이지로 이동
      return null;
    }

    if (!response.ok) {
      throw new Error(`서버 응답 오류: ${response.status}`);
    }

    // console.log('puttt: ', response);
    const responseData = response.headers.get('content-type')?.includes('application/json')
      ? await response.json()
      : null;

    console.log('PUT 응답:', responseData);
    return responseData;
  } catch (error: any) {
    console.error('프로필 업데이트 중 오류 발생:', error);
    alert('업데이트에 실패했습니다.');
  }
};
