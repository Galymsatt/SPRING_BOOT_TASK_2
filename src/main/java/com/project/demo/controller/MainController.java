package com.project.demo.controller;

import com.project.demo.entities.NewsPost;
import com.project.demo.entities.Role;
import com.project.demo.entities.Users;
import com.project.demo.repositories.NewsPostRepository;
import com.project.demo.repositories.RoleRepository;
import com.project.demo.repositories.UserRepository;
import com.project.demo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;


@Controller
public class MainController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserService userService;

    @Autowired
    NewsPostRepository newsPostRepository;

    @GetMapping(value = "/auth_reg")
    public String auth_reg(){
        return "auth_reg";
    }


    @PostMapping(value = "/register")//Users registration
    public String register(@RequestParam(name = "email") String email,
                           @RequestParam(name = "password") String password,
                           @RequestParam(name = "re-password") String re_password,
                           @RequestParam(name = "name") String name,
                           @RequestParam(name = "surName") String surName){

        String redirect = "redirect:/auth_reg?registration_error";

        Users user = userRepository.findByEmail(email);
        if(user==null){

            Set<Role> roles = new HashSet<>();
            Role userRole = roleRepository.getOne(1l);
            roles.add(userRole);

            user = new Users(null, email, password, name, surName, true, roles);
            userService.registerUser(user);//osy zherge kelgen zat kaida ketedi?
            redirect = "redirect:/auth_reg?registration_success";

        }

        return redirect;
    }

    @GetMapping(value = "/profile")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String profile(ModelMap model){

        List<Users> allUsers = userRepository.findAll();
        model.addAttribute("allUsers", allUsers);

        Role moderator = roleRepository.getOne(3L);
        model.addAttribute("moderator", moderator);

        Role admin = roleRepository.getOne(2L);
        model.addAttribute("admin", admin);

        return "profile_admin";
    }

    @GetMapping(value = "/profile_moderator")
    @PreAuthorize("hasRole('ROLE_MODERATOR')")
    public String profile_moderator(ModelMap model){

        List<Users> allUsers = userRepository.findAll();
        model.addAttribute("allUsers", allUsers);

        Role moderator = roleRepository.getOne(3L);
        model.addAttribute("moderator", moderator);

        Role admin = roleRepository.getOne(2L);
        model.addAttribute("admin", admin);

        return "profile_moderator";
    }

    @PostMapping(value = "/addUserModerator")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String addUserModerator(@RequestParam(name = "email") String email,
                                   @RequestParam(name = "password") String password,
                                   @RequestParam(name = "re-password") String re_password,
                                   @RequestParam(name = "name") String name,
                                   @RequestParam(name = "surName") String surName,
                                   @RequestParam(name = "USER") int user_role,
                                   @RequestParam(name = "MODERATOR") int moderator_role){

        String redirect = "redirect:/profile?user/moderator_added_error";

        Users user = userRepository.findByEmail(email);
        if(user==null){

            Set<Role> roles = new HashSet<>();
            roles.add(roleRepository.getOne(1l));
            if(moderator_role==1)
                roles.add(roleRepository.getOne(3L));

            user = new Users(null, email, password, name, surName, true, roles);
            userService.registerUser(user);//osy zherge kelgen zat kaida ketedi?
            redirect = "redirect:/profile?user/moderator_added_success";

        }

        return redirect;
    }

    @PostMapping(value = "/refPassword")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String refPassword(@RequestParam(name = "id") Long id,
                              @RequestParam(name = "password") String password){

        String redirect = "redirect:/profile?password_not_refreshed";

        Optional<Users> user = userRepository.findById(id);
        if(user.isPresent()){
            user.get().setPassword(password);
            userService.registerUser(user.get());
            redirect = "redirect:/profile?password_refreshed";
        }

        return redirect;
    }

    @PostMapping(value = "/blockUser")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MODERATOR')")
    public String blockUser(@RequestParam(name = "id") Long id){

        Optional<Users> user = userRepository.findById(id);
        if(user.isPresent()){
            user.get().setIsActive(false);
            userRepository.save(user.get());
        }


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(!(authentication instanceof AnonymousAuthenticationToken)){
            User secUser = (User)authentication.getPrincipal();
            Users requester = userRepository.findByEmail(secUser.getUsername());
            if(requester.getRoles().contains(roleRepository.getOne(3L)))
                return "redirect:/profile_moderator";
        }


        return "redirect:/profile";
    }

    @PostMapping(value = "/unBlockUser")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MODERATOR')")
    public String unBlockUser(@RequestParam(name = "id") Long id){

        Optional<Users> user = userRepository.findById(id);
        if(user.isPresent()){
            user.get().setIsActive(true);
            userRepository.save(user.get());
        }


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(!(authentication instanceof AnonymousAuthenticationToken)){
            User secUser = (User)authentication.getPrincipal();
            Users requester = userRepository.findByEmail(secUser.getUsername());
            if(requester.getRoles().contains(roleRepository.getOne(3L)))
                return "redirect:/profile_moderator";
        }

        return "redirect:/profile";
    }

    ///////////////////////////////END USER//////////////////////////////////////////

    //////////////////////NEWS POST///////////////////////////////////////////////////////

    @GetMapping(value = "/")
    public String index(ModelMap model){

        List<NewsPost> allNews = newsPostRepository.findAll();
        model.addAttribute("allNews", allNews);
        return "index";
    }

    @PostMapping(value = "/addPost")
    @PreAuthorize("hasRole('ROLE_MODERATOR')")
    public String addPost(@RequestParam(name = "title") String title,
                          @RequestParam(name = "shortContent") String shortContent,
                          @RequestParam(name = "content") String content){

//        Users author = (Users) SecurityContextHolder.getContext().getAuthentication().getPrincipal();//berem avtorizovanny user, nuzno razobratsya kak eto pashet

        Users author = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(!(authentication instanceof AnonymousAuthenticationToken)){
            User secUser = (User)authentication.getPrincipal();
            author = userRepository.findByEmail(secUser.getUsername());
        }

        newsPostRepository.save(new NewsPost(null, title, shortContent, content, author, new Date()));

        return "redirect:/";
    }

    @GetMapping(value = "/newsPage/{id}")
    public String newsPage(ModelMap model,
                           @PathVariable(name = "id") Long id){

        Optional<NewsPost> post = newsPostRepository.findById(id);
        model.addAttribute("post", post.orElse(new NewsPost(null, "No Name", "No Name", "No Name", null, null)));

        return "newsPage";
    }

    @PostMapping("/editPost")
    @PreAuthorize("hasRole('ROLE_MODERATOR')")
    public String editPost(@RequestParam(name = "id") Long id,
                           @RequestParam(name = "title") String title,
                           @RequestParam(name = "shortContent") String shortContent,
                           @RequestParam(name = "content") String content){

        Optional<NewsPost> post = newsPostRepository.findById(id);
        if(post.isPresent()){
            post.get().setTitle(title);
            post.get().setShortContent(shortContent);
            post.get().setContent(content);

            newsPostRepository.save(post.get());
        }


        return "redirect:/newsPage/"+id;
    }


    @PostMapping("/deletePost")
    @PreAuthorize("hasRole('ROLE_MODERATOR')")
    public String deletePost(@RequestParam(name = "id") Long id){

        Optional<NewsPost> post = newsPostRepository.findById(id);
        if(post.isPresent()){
            newsPostRepository.delete(post.get());
        }

        return "redirect:/";
    }


}
