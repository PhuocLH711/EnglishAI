package adu.nttu.englishai.models;

import java.util.ArrayList;
import java.util.List;

// =========================================================================
// DATA REPOSITORY: Kho chứa dữ liệu từ vựng tập trung trong bộ nhớ RAM (Singleton Pattern)
// =========================================================================
public class DataRepository {

    // Biến static lưu trữ thể hiện duy nhất (single instance) của lớp trong suốt vòng đời ứng dụng
    private static DataRepository instance;

    // Danh sách từ vựng được lưu trực tiếp trong RAM điện thoại để các màn hình truy xuất siêu tốc
    private final List<Vocabulary> vocabularyList = new ArrayList<>();

    /*
     * CONSTRUCTOR ĐẶC BIỆT (PRIVATE CONSTRUCTOR):
     * Trong mẫu thiết kế Singleton, constructor bắt buộc phải để private.
     * Ngăn chặn tuyệt đối việc các lớp khác dùng từ khóa `new DataRepository()` tạo ra nhiều kho dữ liệu rác,
     * đảm bảo toàn bộ app chỉ dùng chung đúng 1 nguồn dữ liệu duy nhất (Single Source of Truth).
     */
    private DataRepository() {
        createSampleVocabulary();
    }

    /*
     * HÀM LẤY THỂ HIỆN DUY NHẤT (THREAD-SAFE SINGLETON GETTER):
     * - synchronized: Từ khóa bảo vệ luồng. Nếu có 2 luồng (ví dụ luồng tải Firebase và luồng vẽ giao diện)
     *   cùng gọi getInstance() một lúc, synchronized xếp hàng các luồng, đảm bảo chỉ tạo đúng 1 instance duy nhất,
     *   tránh lỗi xung đột bộ nhớ (Race Condition).
     */
    public static synchronized DataRepository getInstance() {
        if (instance == null) {
            instance = new DataRepository();
        }
        return instance;
    }

    // Hàm lấy danh sách từ vựng ra cho các màn hình Trò chơi (Quiz, Flashcard, MemoryMatch) sử dụng
    public List<Vocabulary> getVocabularyList() {
        return vocabularyList;
    }

    // =========================================================================
    // 🚀 HÀM QUAN TRỌNG VỪA THÊM: NẠP 200 TỪ VỰNG TỪ FIREBASE VÀO RAM
    // =========================================================================
    /*
     * HÀM ĐỒNG BỘ DỮ LIỆU ĐỘNG (DYNAMIC CACHE UPDATER):
     * Ngay khi VocabularyFragment tải thành công 200 từ từ Cloud Firestore,
     * nó sẽ gọi hàm này để xóa sạch 10 từ mẫu cũ đi và thay thế bằng danh sách 200 từ thật.
     * Nhờ đó, tất cả các mini-game khi gọi getVocabularyList() sẽ lập tức có 200 từ mới toanh để chơi!
     */
    public void setVocabularyList(List<Vocabulary> newList) {
        if (newList != null && !newList.isEmpty()) {
            vocabularyList.clear(); // Xóa 10 từ mẫu cũ đi
            vocabularyList.addAll(newList); // Bơm trọn vẹn danh sách mới từ Firebase vào RAM
        }
    }

    // =========================================================================
    // HÀM KHỞI TẠO DỮ LIỆU PHÒNG VỆ (DEFENSIVE FALLBACK DATA)
    // =========================================================================
    /*
     * TẠO 10 TỪ MẪU DỰ PHÒNG (OFFLINE FALLBACK):
     * Nếu học viên vừa mở app ở vùng không có kết nối Internet (chưa tải được Firebase),
     * hàm này đảm bảo trong RAM luôn có sẵn 10 từ vựng cơ bản để ứng dụng không bao giờ bị đơ hay crash
     * khi người dùng bấm vào chơi Quiz hay Lật thẻ. Ngay khi có mạng, hàm setVocabularyList bên trên sẽ ghi đè lại!
     */
    private void createSampleVocabulary() {
        vocabularyList.clear();
        vocabularyList.add(new Vocabulary("1", "Apple", "Quả táo", "/ˈæp.əl/", "I eat an apple every day.", "Food", "Easy"));
        vocabularyList.add(new Vocabulary("2", "Banana", "Quả chuối", "/bəˈnɑː.nə/", "The banana is yellow.", "Food", "Easy"));
        vocabularyList.add(new Vocabulary("3", "Dog", "Con chó", "/dɒɡ/", "The dog is friendly.", "Animals", "Easy"));
        vocabularyList.add(new Vocabulary("4", "Cat", "Con mèo", "/kæt/", "The cat is sleeping.", "Animals", "Easy"));
        vocabularyList.add(new Vocabulary("5", "Teacher", "Giáo viên", "/ˈtiː.tʃər/", "My teacher is very kind.", "School", "Easy"));
        vocabularyList.add(new Vocabulary("6", "Student", "Học sinh", "/ˈstjuː.dənt/", "She is a good student.", "School", "Easy"));
        vocabularyList.add(new Vocabulary("7", "Airplane", "Máy bay", "/ˈeə.pleɪn/", "The airplane is flying.", "Travel", "Medium"));
        vocabularyList.add(new Vocabulary("8", "Beautiful", "Xinh đẹp", "/ˈbjuː.tɪ.fəl/", "The flower is beautiful.", "Adjectives", "Medium"));
        vocabularyList.add(new Vocabulary("9", "Hospital", "Bệnh viện", "/ˈhɒs.pɪ.təl/", "He works at a hospital.", "Places", "Medium"));
        vocabularyList.add(new Vocabulary("10", "Computer", "Máy tính", "/kəmˈpjuː.tər/", "I use a computer for studying.", "Technology", "Easy"));
    }
}