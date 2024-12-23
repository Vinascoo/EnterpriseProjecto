package com.example.projekt_arbete.controller;

import com.example.projekt_arbete.dto.FilmDTO;
import com.example.projekt_arbete.dto.UserFilmDTO;
import com.example.projekt_arbete.model.*;
import com.example.projekt_arbete.response.ErrorResponse;
import com.example.projekt_arbete.response.Response;
import com.example.projekt_arbete.service.IFilmService;
import com.example.projekt_arbete.service.IUserFilmService;
import com.example.projekt_arbete.service.IUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@Controller
public class FilmController {

    private final IFilmService filmService;
    private final IUserService userService;
    private final IUserFilmService userFilmService;

    public FilmController (IFilmService filmService, IUserService userService,

                           IUserFilmService userFilmService) {
        this.filmService = filmService;
        this.userService = userService;

        this.userFilmService = userFilmService;
    }

    @GetMapping("/")
    public String indexPage (Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();

        if(userService.findUserByUsername(username).isPresent()) {

            CustomUser user = userService.findUserByUsername(username).get();

            List<FilmModel> filmList = user.getFilmList();

            model.addAttribute("films", filmList);
            model.addAttribute("username", username);

            return "index";
        }


        return "redirect:/login";

    }

    @GetMapping("/movies/savedfilms")
    public String getSavedFilms (Model model) {

        List<FilmModel> films = filmService.findAll();


        model.addAttribute("films", films);

        return "saved-films";
    }

    @GetMapping("/movies/search")
    public String searchMoviesPage (Model model) {

        model.addAttribute("filmName", "");
        model.addAttribute("film", new FilmDTO());
        model.addAttribute("error", "");

        return "search-page";
    }

    @PostMapping("/movies/search")
    public String searchMovies (@RequestParam String filmName, Model model) {


        List<FilmModel> filmList = filmService.findByTitleContainingIgnoreCase(filmName);

        if (filmList.isEmpty()) {


            model.addAttribute("error", "ingen sån film");

        }

        if (!filmName.isBlank()) {
            model.addAttribute("filmName", filmName);
        }

        model.addAttribute("filmList", filmList);

        return "search-page";
    }

    @GetMapping("/movies/searchid")
    public String searchIdPage () {

        return "searchid-page";
    }


    @PostMapping("/movies/searchid")
    public String search (@RequestParam("filmId") String filmId, Model model) {

        System.out.println("in postMapping for searchid");


        // TODO - plenty! Check the username and password, and change to https, also error handle
        FilmModel film1 = null;

        try {

            ResponseEntity<Response> response = filmService.getFilmById(Integer.parseInt(filmId));

            if (response.getBody() instanceof FilmModel) {
                film1 = (FilmModel) response.getBody();

            } else {

                model.addAttribute("error", "ingen film med id: " + filmId);
                return "searchid-page";
            }

        } catch (Exception e) {

            model.addAttribute("error", "ingen film med id: " + filmId);
            return "searchid-page";
        }

        System.out.println("filmId: " + filmId);

        model.addAttribute("film", film1);


        return "searchid-page";
    }

    @PostMapping("/movies/getfilm")
    public String getFilm (@ModelAttribute FilmModel film, Model model) {

        //TODO - go to searchid-page.html to include more film parameters, or consider using a DTO..
        System.out.println("film.title: " + film.getTitle());
        System.out.println("film.id: " + film.getId());
        System.out.println("film.poster_path: " + film.getPoster_path());

        List<FilmModel> userFilms = userService.findUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).get().getFilmList();

        for (FilmModel filmModel : userFilms) {

            if (Objects.equals(filmModel.getTitle(), film.getTitle())) {

                model.addAttribute("saved", "filmen är redan sparad");
            }

        }

        model.addAttribute("film", film);
        return "film-details";

    }


    @PostMapping("/movies/savefilm")
    public String saveFilm (@ModelAttribute FilmModel filmModel, Model model) throws IOException {



        int filmId = filmModel.getId();

        try {

            if (filmService.getFilmById( filmId).getBody() instanceof FilmModel) {

                filmService.saveFilmById("movie", filmId);

            } else {

                model.addAttribute("error", "ingen film med id: " + filmId);
                return "searchid-page";
            }


        } catch (Exception e) {

            model.addAttribute("error", "ingen film med id: " + filmId);
            return "searchid-page";
        }

        return "redirect:/";
    }

    @GetMapping("/opinion/{id}")
    public String toOpinionPage (@PathVariable Integer id, Model model) {

        FilmModel film = filmService.getFilmById(id).get();
        CustomUser currentUser = userService.findUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).get();

        Optional<UserFilm> optionalUserFilm = userFilmService.findByFilmModelAndCustomUser(film, currentUser);

        UserFilm userFilm = null;

        UserFilmDTO userFilmDTO = new UserFilmDTO();

        if (optionalUserFilm.isPresent()) {

            userFilm = optionalUserFilm.get();
            userFilmDTO.setOpinion(userFilm.getOpinion());

        }

        userFilmDTO.setId(film.getFilmid());
        userFilmDTO.setTitle(film.getTitle());


        model.addAttribute("film", userFilmDTO);

        return "opinion-page";
    }

    @PostMapping("/opinion")
    public String addOpinion (@ModelAttribute("film") UserFilmDTO film, Model model) {

        filmService.addOpinion(film.getId(), film.getOpinion());

        CustomUser user = userService.findUserByUsername("test").get();


        model.addAttribute("film", film);

        return "opinion-page";
    }

    @GetMapping("/movies/info")
    public String getInfo (Model model) {

        ErrorResponse info = (ErrorResponse) filmService.getInfo().getBody();

        System.out.println("info : " + info.getResponseMessage());

        model.addAttribute("info", info.getResponseMessage());

        return "info-page";
    }

    @GetMapping("/movies/info/{id}")
    public String getFilmInfo (@PathVariable int id,
                               Model model) {

        FilmDTO filmDTO = (FilmDTO) filmService.getFilmWithAdditionalInfo(id, true, true).getBody();

        model.addAttribute("film", filmDTO);

        return "film-info";
    }

    @PostMapping("/movies/delete/{id}")
    public String deleteFilm (@PathVariable int id) throws Exception {

        filmService.deleteById(id);

        return "redirect:/movies/savedfilms";
    }

    @GetMapping("/movies/savedfilms/{id}")
    public String getFilm (@PathVariable int id, Model model) {

        FilmModel film = (FilmModel) filmService.findById(id).getBody();

        List<FilmModel> userFilms = userService.findUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).get().getFilmList();

        for (FilmModel filmModel : userFilms) {

            if (Objects.equals(filmModel.getTitle(), film.getTitle())) {

                model.addAttribute("saved", "Du har filmen sparad");
            }

        }


        model.addAttribute("film", film);
        return "movie-details";
    }


}
