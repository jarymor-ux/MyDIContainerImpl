package ru.ostap;
@Component
public class UserService {

    private final Repository repository;

    @Autowired
    public UserService(Repository repository) {
        this.repository = repository;
    }


    public void saveData(String data) {
        repository.save(data);
    }
}
