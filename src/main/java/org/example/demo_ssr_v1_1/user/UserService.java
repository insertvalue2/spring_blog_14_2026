package org.example.demo_ssr_v1_1.user;

import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1_1._core.errors.exception.Exception400;
import org.example.demo_ssr_v1_1._core.errors.exception.Exception403;
import org.example.demo_ssr_v1_1._core.errors.exception.Exception404;
import org.example.demo_ssr_v1_1._core.utils.FileUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    // 비밀번호 암호화를 위한 PasswordEncoder 주입
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 처리 (프로필 이미지 포함)
     * 
     * 비즈니스 로직:
     * 1. 유효성 검사 (DTO에서 처리)
     * 2. 사용자명 중복 체크
     * 3. 프로필 이미지 저장 (선택사항)
     * 4. 기본 권한(USER) 추가
     * 5. 엔티티 저장
     * 
     * 트랜잭션:
     * - 기본 트랜잭션 (읽기/쓰기)
     * - save() 메서드 실행 시 INSERT 쿼리 실행
     * 
     * @param joinDTO 회원가입 DTO (프로필 이미지 포함)
     * @return 저장된 사용자 엔티티
     * @throws Exception400 사용자명이 이미 존재할 경우 또는 파일 저장 실패 시
     */
    @Transactional
    public User 회원가입(UserRequest.JoinDTO joinDTO) {
        // 1. 유효성 검사
        joinDTO.validate();

        // 2. 사용자명 중복 체크
        // Optional의 isPresent(): 값이 있으면 true, 없으면 false
        if (userRepository.findByUsername(joinDTO.getUsername()).isPresent()) {
            throw new Exception400("이미 존재하는 사용자 이름입니다");
        }

        // 3. 프로필 이미지 저장 (선택사항)
        // 중요: 프로필 이미지는 필수가 아닌 선택사항입니다!
        // 사용자가 이미지를 업로드하지 않아도 회원가입은 정상적으로 진행됩니다.
        // 
        // 파일 업로드 처리 흐름:
        // 1) joinDTO.getProfileImage()가 null이거나 비어있으면 → 이미지 없이 회원가입 진행
        // 2) 파일이 있으면 → 파일 검증 → 파일 저장 → 파일명을 DB에 저장
        String profileImageFilename = null;  // 초기값은 null (이미지 없음)
        
        // 파일이 업로드되었는지 확인
        // MultipartFile의 isEmpty() 메서드: 파일이 없거나 크기가 0이면 true
        if (joinDTO.getProfileImage() != null && !joinDTO.getProfileImage().isEmpty()) {
            try {
                // 3-1. 이미지 파일인지 검증
                // Content-Type이 "image/"로 시작하는지 확인 (예: image/jpeg, image/png)
                if (!FileUtil.isImageFile(joinDTO.getProfileImage())) {
                    throw new Exception400("이미지 파일만 업로드 가능합니다");
                }
                
                // 3-2. 파일을 서버 디스크에 저장
                // FileUtil.saveFile() 메서드가 하는 일:
                // - UUID를 사용하여 고유한 파일명 생성 (중복 방지)
                // - "images/" 디렉토리에 파일 저장
                // - 저장된 파일명 반환 (예: "abc123-456-789-profile.jpg")
                profileImageFilename = FileUtil.saveFile(joinDTO.getProfileImage(), FileUtil.IMAGES_DIR);
                
                // 3-3. profileImageFilename에는 저장된 파일명이 들어있음
                // 이 파일명을 DB의 user_tb.profile_image 컬럼에 저장할 예정
            } catch (IOException e) {
                // 파일 저장 중 오류 발생 시 (예: 디스크 공간 부족, 권한 없음)
                throw new Exception400("파일 저장에 실패했습니다: " + e.getMessage());
            }
        }
        // 파일이 없으면 profileImageFilename은 null로 유지됨
        // → DB에 null로 저장되어 "프로필 이미지 없음" 상태가 됨

        // 4. 비밀번호 암호화 처리
        // 회원가입 요청자가 제출한 password를 BCrypt로 암호화
        // BCrypt는 단방향 해싱 알고리즘이므로 복호화 불가능
        // 검증 시에는 passwordEncoder.matches() 메서드 사용
        String hashPwd = passwordEncoder.encode(joinDTO.getPassword());
        
        // 5. DTO를 엔티티로 변환 (암호화된 비밀번호와 파일명 포함)
        User user = joinDTO.toEntity(profileImageFilename);
        // 암호화된 비밀번호로 교체
        user.setPassword(hashPwd);

        // 6. 기본 권한 추가 (일반 사용자, 생성자에서 넣고 있음)
        // 회원가입 시 기본적으로 USER 역할을 부여합니다.
        // user.addRole(Role.USER);

        // 7. JpaRepository의 save() 메서드: 엔티티 저장 (INSERT)
        return userRepository.save(user);
    }

    /**
     * 로그인 처리
     * 
     * 비즈니스 로직:
     * 1. 유효성 검사 (DTO에서 처리)
     * 2. 사용자명으로 사용자 조회 (+ 역할 정보까지 함께 조회)
     * 3. 비밀번호 검증 (BCrypt matches 메서드 사용)
     * 4. 로그인 성공/실패 처리
     * 
     * 비밀번호 검증 방식:
     * - DB에 저장된 비밀번호는 BCrypt로 암호화된 해시값
     * - 사용자가 입력한 평문 비밀번호와 암호화된 비밀번호를 비교
     * - passwordEncoder.matches(평문비밀번호, 암호화된비밀번호) 사용
     * 
     * 트랜잭션:
     * - 읽기 전용 트랜잭션 (readOnly = true)
     * - 조회만 하므로 읽기 전용으로 설정
     * 
     * @param loginDTO 로그인 DTO
     * @return 로그인한 사용자 엔티티
     * @throws Exception400 로그인 실패 시 (사용자명 또는 비밀번호 불일치)
     */
    @Transactional(readOnly = true)
    public User 로그인(UserRequest.LoginDTO loginDTO) {
        // 1. 유효성 검사
        loginDTO.validate();

        // 2. 사용자명으로 사용자 조회 (+ 역할 정보까지 함께 조회)
        //    findByUsernameWithRoles():
        //    - User 엔티티와 roles 컬렉션을 LEFT JOIN FETCH로 한 번에 가져옵니다.
        //    - 세션에 저장된 User에서 isAdmin(), getRoleDisplay() 등을 사용할 수 있습니다.
        //    - 비밀번호는 DB에서 직접 비교하지 않고 애플리케이션에서 검증
        User user = userRepository.findByUsernameWithRoles(loginDTO.getUsername())
                .orElse(null);

        // 3. 사용자가 존재하지 않으면 로그인 실패
        if (user == null) {
            throw new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다");
        }

        // 4. 비밀번호 검증 (BCrypt matches 메서드 사용)
        // passwordEncoder.matches(평문비밀번호, 암호화된비밀번호)
        // - 사용자가 입력한 평문 비밀번호와 DB에 저장된 암호화된 비밀번호를 비교
        // - BCrypt 알고리즘이 자동으로 Salt를 고려하여 비교
        // - 일치하면 true, 불일치하면 false 반환
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다");
        }

        // 5. 로그인 성공: 사용자 엔티티 반환
        return user;
    }

    /**
     * 회원정보 수정 화면용 조회 (인가 검사 포함)
     * 
     * 인가 검사:
     * - 자기 자신의 정보만 조회 가능
     * - isOwner() 메서드로 소유자 확인
     * 
     * @param userId 현재 로그인한 사용자 ID
     * @return 사용자 엔티티
     * @throws Exception404 사용자가 없을 경우
     * @throws Exception403 수정 권한이 없을 경우
     */
    @Transactional(readOnly = true)
    public User 회원정보수정화면(Long userId) {
        // 세션의 사용자 ID로 회원정보 조회
        // JpaRepository의 findById()는 Optional<User>를 반환
        // orElseThrow(): Optional이 비어있으면 예외 발생, 있으면 User 객체 반환
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("사용자를 찾을 수 없습니다"));

        // 자기 자신의 정보만 수정 가능한지 확인
        if (!user.isOwner(userId)) {
            throw new Exception403("회원정보 수정 권한이 없습니다");
        }

        return user;
    }

    /**
     * 회원정보 수정 처리 (프로필 이미지 포함)
     * 
     * 더티 체킹 (Dirty Checking):
     * - 엔티티를 조회한 후 필드 값을 변경
     * - 트랜잭션이 끝날 때 자동으로 UPDATE 쿼리 실행
     * - save()를 호출해도 되지만, @Transactional이 있으면 자동으로 UPDATE 됨
     * 
     * 세션 갱신:
     * - 수정된 사용자 정보를 세션에 다시 저장
     * - Controller에서 처리하도록 엔티티 반환
     * 
     * @param updateDTO 회원정보 수정 DTO (프로필 이미지 포함)
     * @param userId 현재 로그인한 사용자 ID
     * @return 수정된 사용자 엔티티
     * @throws Exception400 파일 저장 실패 시
     * @throws Exception404 사용자가 없을 경우
     * @throws Exception403 수정 권한이 없을 경우
     */
    @Transactional
    public User 회원정보수정(UserRequest.UpdateDTO updateDTO, Long userId) {
        // 1. 수정하려는 회원정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("사용자를 찾을 수 없습니다"));

        // 2. 인가 검사: 자기 자신의 정보만 수정 가능한지 확인
        if (!user.isOwner(userId)) {
            throw new Exception403("회원정보 수정 권한이 없습니다");
        }

        // 3. 유효성 검사
        updateDTO.validate();

        // 4. 비밀번호 암호화 처리
        // 회원정보 수정 시 입력한 비밀번호를 BCrypt로 암호화
        String hashPwd = passwordEncoder.encode(updateDTO.getPassword());
        // DTO의 비밀번호를 암호화된 비밀번호로 교체
        updateDTO.setPassword(hashPwd);

        // 5. 프로필 이미지 처리 (새로운 이미지가 업로드된 경우)
        // 중요: 프로필 이미지 수정도 선택사항입니다!
        // 사용자가 새 이미지를 업로드하지 않으면 기존 이미지를 유지합니다.
        //
        // 프로필 이미지 수정 시나리오:
        // 1) 새 이미지 업로드 → 새 이미지 저장 → 기존 이미지 삭제 → DB 업데이트
        // 2) 이미지 업로드 안 함 → 기존 이미지 유지 → DB 변경 없음
        
        String oldProfileImage = user.getProfileImage();  // 기존 이미지 파일명 저장 (나중에 삭제하기 위해)
        
        // 새 이미지가 업로드되었는지 확인
        if (updateDTO.getProfileImage() != null && !updateDTO.getProfileImage().isEmpty()) {
            try {
                // 4-1. 이미지 파일인지 검증
                if (!FileUtil.isImageFile(updateDTO.getProfileImage())) {
                    throw new Exception400("이미지 파일만 업로드 가능합니다");
                }
                
                // 4-2. 새 이미지를 서버 디스크에 저장
                // UUID를 사용하여 고유한 파일명으로 저장
                String newProfileImageFilename = FileUtil.saveFile(updateDTO.getProfileImage(), FileUtil.IMAGES_DIR);
                
                // 4-3. DTO에 새 파일명 설정 (나중에 엔티티 업데이트 시 사용)
                updateDTO.setProfileImageFilename(newProfileImageFilename);
                
                // 4-4. 기존 이미지 파일 삭제 (디스크 공간 절약)
                // 주의: DB의 파일명만 삭제하는 것이 아니라 실제 파일도 삭제해야 함!
                // 그렇지 않으면 디스크에 사용하지 않는 파일이 계속 쌓임
                if (oldProfileImage != null && !oldProfileImage.isEmpty()) {
                    FileUtil.deleteFile(oldProfileImage, FileUtil.IMAGES_DIR);
                }
            } catch (IOException e) {
                throw new Exception400("파일 저장에 실패했습니다: " + e.getMessage());
            }
        } else {
            // 새 이미지가 업로드되지 않았으면 기존 이미지 파일명 유지
            // → DB의 profile_image 컬럼 값이 변경되지 않음
            updateDTO.setProfileImageFilename(oldProfileImage);
        }

        // 6. 더티 체킹을 활용한 수정 처리
        // 엔티티의 상태 값 변경
        user.update(updateDTO);

        // 7. 변경된 엔티티 저장 (더티 체킹)
        // 참고: @Transactional이 있으면 save() 없이도 자동으로 UPDATE 됨
        // 하지만 명시적으로 save()를 호출하는 것이 더 명확함
        User updateUser = userRepository.save(user);

        // 8. 수정된 사용자 정보 반환 (Controller에서 세션 갱신용)
        return updateUser;
    }

    /**
     * 프로필 이미지 삭제 처리
     * 
     * 비즈니스 로직:
     * 1. 회원정보 조회
     * 2. 인가 검사 (소유자 확인)
     * 3. 프로필 이미지 파일 삭제
     * 4. DB에서 프로필 이미지 필드 null로 업데이트
     * 
     * @param userId 현재 로그인한 사용자 ID
     * @return 프로필 이미지가 삭제된 사용자 엔티티
     * @throws Exception404 사용자가 없을 경우
     * @throws Exception403 삭제 권한이 없을 경우
     */
    @Transactional
    public User 프로필이미지삭제(Long userId) {
        // 1. 회원정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("사용자를 찾을 수 없습니다"));

        // 2. 인가 검사: 자기 자신의 정보만 삭제 가능한지 확인
        if (!user.isOwner(userId)) {
            throw new Exception403("프로필 이미지 삭제 권한이 없습니다");
        }

        // 3. 프로필 이미지 파일 삭제 (있으면)
        String profileImage = user.getProfileImage();
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                FileUtil.deleteFile(profileImage, FileUtil.IMAGES_DIR);
            } catch (IOException e) {
                // 파일 삭제 실패해도 DB는 업데이트 (파일이 이미 없을 수도 있음)
                // 로그만 남기고 계속 진행
                System.err.println("프로필 이미지 파일 삭제 실패: " + e.getMessage());
            }
        }

        // 4. DB에서 프로필 이미지 필드 null로 업데이트
        user.setProfileImage(null);
        
        // 5. 변경된 엔티티 저장 (더티 체킹)
        return userRepository.save(user);
    }

    /**
     * 사용자명으로 조회 (소셜 로그인용)
     */
    @Transactional(readOnly = true)
    public User 사용자이름조회(String username) {
        // Optional 처리: 없으면 null 반환
        return userRepository.findByUsername(username).orElse(null);
    }


    public void 소셜회원가입(User user) {
        userRepository.save(user);   // 그냥 저장만 하면 됨
    }
}

