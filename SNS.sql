DROP DATABASE IF EXISTS sns;

CREATE DATABASE sns;

USE sns;

-- 1. USER 테이블
CREATE TABLE USER (
    User_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Email VARCHAR(100) UNIQUE NOT NULL,
    Password VARCHAR(255) NOT NULL,
    Biography TEXT
);

-- 2. POST 테이블
CREATE TABLE POST (
    Post_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    User_id INT NOT NULL,
    Post_context TEXT,
    File_path VARCHAR(255),
    File_name VARCHAR(255),
    Upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    Update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (User_id) REFERENCES USER (User_id) ON UPDATE CASCADE ON DELETE CASCADE
);

-- 3. IMAGE 테이블
CREATE TABLE IMAGE (
    Image_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    Post_id INT NOT NULL,
    File_path VARCHAR(255),
    File_name VARCHAR(255),
    FOREIGN KEY (Post_id) REFERENCES POST (Post_id) ON UPDATE CASCADE ON DELETE CASCADE
);

-- 4. COMMENT 테이블
CREATE TABLE COMMENT (
    Comment_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    Post_id INT NOT NULL,
    User_id INT NOT NULL,
    Comment TEXT NOT NULL,
    Upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    Update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (Post_id) REFERENCES POST (Post_id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (User_id) REFERENCES USER (User_id) ON UPDATE CASCADE ON DELETE CASCADE
);

-- 5. REACTION 테이블
CREATE TABLE REACTION (
    User_id INT NOT NULL,
    Post_id INT NOT NULL,
    Type ENUM('LIKE', 'DISLIKE') NOT NULL,
    PRIMARY KEY (User_id, Post_id),
    FOREIGN KEY (User_id) REFERENCES USER (User_id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (Post_id) REFERENCES POST (Post_id) ON UPDATE CASCADE ON DELETE CASCADE
);

SHOW TABLES;

SELECT * FROM USER;

USE sns;

-- 1. 사용자 (USER) 데이터 생성 (5명)
-- 비밀번호는 모두 '1234'로 통일했습니다.
INSERT INTO
    USER (
        Name,
        Email,
        Password,
        Biography
    )
VALUES (
        'Jinyoung',
        'jin@test.com',
        '1234',
        'Backend Developer 지망생입니다.'
    ),
    (
        'Minji',
        'min@test.com',
        '1234',
        '디자인과 코딩을 좋아해요.'
    ),
    (
        'Namjin',
        'nam@test.com',
        '1234',
        'SQL 쿼리 마스터'
    ),
    (
        'GuestUser',
        'guest@test.com',
        '1234',
        '구경 왔습니다.'
    ),
    (
        'Professor',
        'prof@test.com',
        '1234',
        '과제 채점용 계정'
    );

-- 2. 게시글 (POST) 데이터 생성 (5개)
-- 텍스트만 있는 글, 이미지가 있는 글을 섞었습니다.
INSERT INTO
    POST (
        User_id,
        Post_context,
        File_name,
        Upload_time
    )
VALUES (
        1,
        '드디어 DB 워크샵 프로젝트 끝! 다들 너무 고생했어.',
        NULL,
        '2023-11-28 14:00:00'
    ),
    (
        2,
        '우리집 고양이 보고 가세요. 너무 귀엽죠?',
        'cat.jpg',
        '2023-11-28 15:30:00'
    ),
    (
        3,
        '새벽 코딩 중... SQL JOIN이 왜 이렇게 안 되지?',
        'code_error.png',
        '2023-11-29 01:00:00'
    ),
    (
        1,
        '오늘 점심은 학식 돈까스! 가성비 최고.',
        'lunch.png',
        '2023-11-29 12:30:00'
    ),
    (
        4,
        '안녕하세요. 가입 인사 드립니다! 잘 부탁드려요.',
        NULL,
        '2023-11-30 09:00:00'
    );

-- 3. 댓글 (COMMENT) 데이터 생성 (5개)
INSERT INTO
    COMMENT (Post_id, User_id, Comment)
VALUES (1, 2, '진짜 고생했다! 발표 준비만 잘 하자.'),
    (1, 3, '오늘 저녁에 회식 고고?'),
    (
        2,
        1,
        '와 진짜 귀엽다... 나만 고양이 없어.'
    ),
    (3, 2, '오타 난 거 아니야? 스펠링 확인해봐.'),
    (5, 1, '반갑습니다! 환영해요.');

-- 4. 반응 (REACTION) 데이터 생성 (좋아요/싫어요)
-- 1번 게시글(프로젝트 끝)에 좋아요를 많이 배치하여 '인기글' 쿼리 시연에 좋게 만듦
INSERT INTO
    REACTION (User_id, Post_id, Type)
VALUES (2, 1, 'LIKE'),
    (3, 1, 'LIKE'),
    (4, 1, 'LIKE'), -- 1번 글: 좋아요 3개
    (1, 2, 'LIKE'),
    (3, 2, 'LIKE'), -- 2번 글: 좋아요 2개
    (2, 3, 'LIKE'),
    (4, 3, 'DISLIKE'), -- 3번 글: 좋아요 1, 싫어요 1
    (1, 5, 'LIKE');
-- 5번 글: 좋아요 1개

-- [검색 기능] 키워드로 본문, 파일명, 작성자 찾기
SELECT p.Post_id, u.Name AS Author, p.Post_context, p.File_name
FROM POST p
    JOIN USER u ON p.User_id = u.User_id
WHERE
    p.Post_context LIKE '%돈까스%'
    OR p.File_name LIKE '%돈까스%'
    OR u.Name LIKE '%돈까스%'
ORDER BY p.Post_id DESC;