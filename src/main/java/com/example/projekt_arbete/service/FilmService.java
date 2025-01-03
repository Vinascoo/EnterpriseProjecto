package com.example.projekt_arbete.service;

import com.example.projekt_arbete.client.FilmApiClient;
import com.example.projekt_arbete.dao.IFilmDAO;
import com.example.projekt_arbete.model.CustomUser;
import com.example.projekt_arbete.dto.FilmDTO;
import com.example.projekt_arbete.model.FilmModel;
import com.example.projekt_arbete.model.UserFilm;
import com.example.projekt_arbete.response.ErrorResponse;
import com.example.projekt_arbete.response.IntegerResponse;
import com.example.projekt_arbete.response.ListResponse;
import com.example.projekt_arbete.response.Response;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.*;


@Service
public class FilmService implements IFilmService{


    private String ApiKey="df0eb7f0729911f3781785d3811ec8dd";


    private final IFilmDAO filmDao;

    private final IUserFilmService userFilmService;

    private final IUserService userService;

    private final FilmApiClient filmApiClient;
    private final RateLimiter rateLimiter;

    @Autowired
    public FilmService (
                        FilmApiClient filmApiClient,
                        IUserFilmService userFilmService,
                        IFilmDAO filmDao, IUserService userService, RateLimiter rateLimiter) {


        this.filmDao = filmDao;
        this.userService = userService;
        this.rateLimiter = rateLimiter;
        this.filmApiClient = filmApiClient;
        this.userFilmService = userFilmService;
    }

    @Override
    public ResponseEntity<Response> getFilmById(int id) {

        Optional<FilmModel> film = filmApiClient.getFilmById(id);

        if (film.isPresent()) {
            return ResponseEntity.ok(film.get());
        } else {
            return ResponseEntity.status(404).body(new ErrorResponse("Film inte funnen"));
        }


    }

    @Override
    public ResponseEntity<Response> saveFilmById(@RequestParam(defaultValue = "movie") String movie, @PathVariable int id) throws IOException {

        Optional<FilmModel> optionalFilm = filmApiClient.getFilmById(id);

        if (optionalFilm.isPresent()) {

            FilmModel film = optionalFilm.get();


            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();



            CustomUser user;

            if (authentication != null) {
                String username = authentication.getName();

                user = userService.findUserByUsername(username).get();
            } else {

                user = userService.findUserByUsername("test").get();
            }


            System.out.println("film.getId: " + film.getId());
            System.out.println("film.getfilmid: " + film.getFilmid());

            List<FilmModel> allFilms = findAll();

            for (FilmModel film1 : allFilms) {

                if (film1.getId() == film.getId()) {

                    FilmModel currentFilm = findByTitle(film.getTitle()).get();


                    List<CustomUser> customUserList = currentFilm.getCustomUsers();
                    customUserList.add(user);
                    currentFilm.setCustomUsers(customUserList);

                    return ResponseEntity.ok(filmDao.save(currentFilm));

                }

            }

            List<CustomUser> customUserList = new ArrayList<>();
            customUserList.add(user);

            film.setCustomUsers(customUserList);
            return ResponseEntity.ok(filmDao.save(film));



        } else {
            return ResponseEntity.status(404).body(new ErrorResponse("Film inte funnen"));
        }

    }



    @Override
    public List<FilmModel> findAll () {
        return filmDao.findAll();
    }

    @Override
    public ResponseEntity<Response> findById (Integer id) {

        try {

            Optional<FilmModel> optionalFilm = filmDao.findById(id);

            if (optionalFilm.isPresent()) {

                return ResponseEntity.ok((optionalFilm.get()));
            } else {

                return ResponseEntity.status(404).body(new ErrorResponse("film finns inte"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("något fel"));
        }

    }

    @Override
    public Optional<FilmModel> getFilmById(Integer id) {
        return filmDao.findById(id);
    }

    @Override
    public ResponseEntity<String> deleteById (Integer id) throws Exception {

        // TODO - why this? Use in if statement perhaps
        Optional<FilmModel> optionalFilm = filmDao.findById(id);

        try {

            if (filmDao.findById(id).isPresent()) {

                filmDao.deleteById(id);
                return ResponseEntity.ok("Film with id "+ id + " removed");

            } else {

                return ResponseEntity.status(404).body("no film found with id: " + id);
            }
        } catch (Exception e) {
            throw new Exception();
        }
    }

    @Override
    public ResponseEntity<Response> changeCountryOfOrigin (int id, String country) {

        List<String> newCountryOfOrigins = new ArrayList<>() {};

        newCountryOfOrigins.add(country);

        Optional<FilmModel> filmOptional = filmDao.findById(id);

        if (filmOptional.isEmpty()) {
            return ResponseEntity.status(404).body(new ErrorResponse("Film finns inte! <@:)"));
        }

        try {

            FilmModel film = filmOptional.get();

            film.setOrigin_country(newCountryOfOrigins);

            filmDao.save(film);

            return ResponseEntity.ok(film);

        } catch (NoSuchElementException e) {

            return ResponseEntity.status(404).body(new ErrorResponse("film finns inte"));
        }

    }

    @Override
    public ResponseEntity<Response> searchFilmByName (String filmName) {

        try {

            if (filmName == null || filmName.isBlank()) {
                return ResponseEntity.status(400).body(new ErrorResponse("Du måste skriva namn"));
            }



            List<FilmModel> allFilms = filmDao.findAll();

            for (FilmModel film : allFilms) {


                if (film.getTitle().equals(filmName)) {

                    FilmDTO filmDto = new FilmDTO();
                    filmDto.setTitle(film.getTitle());
                    filmDto.setId((long) film.getFilmid());


                    return ResponseEntity.ok(filmDto);
                }
            }

            return ResponseEntity.status(404).body(new ErrorResponse("No movie found with name: " + filmName));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("something wrong"));
        }
    }

    @Override
    public ResponseEntity<Response> getFilmByCountry (String country, String title) {

        if (rateLimiter.acquirePermission()) {

            List<FilmModel> savedFilms = filmDao.findAll();

            List<FilmModel> filmsByCountry = new ArrayList<>();

            try {


                if (title == null || title.isBlank()) {

                    for (FilmModel film : savedFilms) {

                        if (film.getOrigin_country().get(0).equals(country.toUpperCase())) {

                            filmsByCountry.add(film);
                        }
                    }

                    return ResponseEntity.ok(new ListResponse(filmsByCountry));
                }

                for (FilmModel film : savedFilms) {

                    if (film.getOrigin_country().get(0).equals(country.toUpperCase()) && film.getOriginal_title().equals(title)) {

                        return ResponseEntity.ok(film);
                    }
                }

                return ResponseEntity.status(400).body(new ErrorResponse("movie does not exist: " + title));
            } catch (Exception e) {
                return ResponseEntity.status(500).body(new ErrorResponse("something wrong"));
            }

        } else {
            return ResponseEntity.status(429).body(new ErrorResponse("too many requests"));
        }
    }

    @Override
    public ResponseEntity<Response> getAverageRuntime () {

        try {

            List<FilmModel> films = filmDao.findAll();
            if (films.isEmpty()) {
                return ResponseEntity.status(404).body(new ErrorResponse("inga filmer sparade än"));
            }

            int runtimeInMin = 0;

            for (FilmModel film : films) {

                runtimeInMin += film.getRuntime();

            }

            return ResponseEntity.ok(new IntegerResponse(runtimeInMin / filmDao.findAll().size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("something wrong"));
        }
    }

    @Override
    public ResponseEntity<String> addOpinion (Integer id, String opinion) {

        try {
            if (opinion == null || opinion.isEmpty() || opinion.isBlank()) {
                return ResponseEntity.status(400).body("need body");
            }

            FilmModel optionalFilm = filmDao.findById(id).get();
            CustomUser optionalCustomUser = userService.findUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).get();

            Optional<UserFilm> existingUserFilm = userFilmService.findByFilmModelAndCustomUser(optionalFilm, optionalCustomUser);

            if (existingUserFilm.isPresent()) {

                UserFilm userFilm = existingUserFilm.get();
                userFilm.setOpinion(opinion);

                userFilmService.saveUserFilm(userFilm);

                return ResponseEntity.ok().body("Opinion updated");
            } else {

                UserFilm newUserFilm = new UserFilm();
                newUserFilm.setFilmModel(optionalFilm);
                newUserFilm.setCustomUser(optionalCustomUser);
                newUserFilm.setOpinion(opinion);

                userFilmService.saveUserFilm(newUserFilm);

                return ResponseEntity.status(201).body("Opinion added");

            }


        } catch (Exception e) {
            return ResponseEntity.status(500).body("something wrong");
        }
    }

    @Override
    public ResponseEntity<Response> getFilmWithAdditionalInfo(int filmId, boolean opinion, boolean description) {

        if (rateLimiter.acquirePermission()) {

            FilmModel film;
            FilmDTO filmDTO = new FilmDTO();
            try {

                if (filmDao.findById(filmId).isPresent()) {
                    film = filmDao.findById(filmId).get();
                } else {
                    return ResponseEntity.status(404).body(new ErrorResponse("Film finns inte"));
                }

                if (opinion == true && description == true) {
                    filmDTO.setDescription(film.getOverview());

                    if (userFilmService.findByFilmModelAndCustomUser(film, userService.findUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).get()).isPresent()) {
                        filmDTO.setOpinion(userFilmService.findByFilmModelAndCustomUser(film, userService.findUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).get()).get().getOpinion());
                    }

                    filmDTO.setTitle(film.getTitle());

                    return ResponseEntity.ok(filmDTO);
                }

                if (opinion == true) {
                    filmDTO.setTitle(film.getTitle());
                    if (userFilmService.findByFilmModelAndCustomUser(film, userService.findUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).get()).isPresent()) {
                        filmDTO.setOpinion(userFilmService.findByFilmModelAndCustomUser(film, userService.findUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).get()).get().getOpinion());
                    }

                    filmDTO.setDescription("nothing here");

                    return ResponseEntity.ok(filmDTO);

                }

                if (description == true) {
                    filmDTO.setTitle(film.getTitle());
                    filmDTO.setDescription(film.getOverview());
                    filmDTO.setOpinion("nothing here");

                    return ResponseEntity.ok(filmDTO);
                }
                filmDTO.setTitle(film.getTitle());
                filmDTO.setDescription("nothing here");
                filmDTO.setOpinion("nothing here");

                return ResponseEntity.ok(filmDTO);
            } catch (Exception e) {
                return ResponseEntity.status(500).body(new ErrorResponse("something wrong with the database"));
            }

        } else {
            return ResponseEntity.status(429).body(new ErrorResponse("too many requests"));
        }

    }

    @Override
    public ResponseEntity<Response> getInfo() {

        if (rateLimiter.acquirePermission()) {

            int AmericanMovies = 0;
            int NonAmericanMovies = 0;

            try {


                List<FilmModel> films = findAll();
                Collections.sort(films, new Comparator<FilmModel>() {
                    @Override
                    public int compare(FilmModel o1, FilmModel o2) {
                        return Integer.compare(o1.getBudget(), o2.getBudget());
                    }
                });

                for (FilmModel film : films) {

                    if (Objects.equals(film.getOrigin_country().get(0), "US")) {
                        AmericanMovies++;
                    } else {
                        NonAmericanMovies++;
                    }

                    System.out.println(film.getOriginal_title() + ": "  + " origin country " + film.getOrigin_country().get(0));
                    ;
                }


                if (findAll().isEmpty()) {
                    return ResponseEntity.ok(new ErrorResponse("Du har inga sparade filmer"));
                }


                IntegerResponse intRes = (IntegerResponse) getAverageRuntime().getBody();
                int averageRuntime = intRes.getAverageRuntime();

                return ResponseEntity.ok(new ErrorResponse("There are: " + findAll().size() + " movies saved." + "\n\r" +
                        " average runtime for the movies are: " + averageRuntime + " minutes, " +
                        "where " +   " of these are " + AmericanMovies + " american and the rest " + NonAmericanMovies + " from other countries"));


            } catch (Exception e) {
                return ResponseEntity.status(500).body(new ErrorResponse("something wrong"));
            }
        } else {
            return ResponseEntity.status(429).body(new ErrorResponse("för många förfrågan"));
        }

    }

    @Override
    public Optional<FilmModel> findByTitle(String filmName) {

        return filmDao.findByTitle(filmName);
    }

    @Override
    public Optional<FilmModel> findByTitleIgnoreCase(String filmName) {

        return filmDao.findByTitleIgnoreCase(filmName);


    }

    @Override
    public List<FilmModel> findByTitleContainingIgnoreCase(String filmName) {

        return filmDao.findByTitleContainingIgnoreCase(filmName);
    }



}
