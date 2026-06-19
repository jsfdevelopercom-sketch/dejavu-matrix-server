package com.dejavu.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    @GetMapping("/registry")
    public ResponseEntity<Map<String, List<String>>> getAssetRegistry() {
        List<String> backgrounds = List.of(
            "bg_generic_dark_room_01", "bg_rain_window_room_01", "bg_school_classroom_01", 
            "bg_school_corridor_01", "bg_hospital_corridor_01", "bg_hospital_room_01", 
            "bg_bathroom_mirror_01", "bg_old_hostel_room_01", "bg_bedroom_moonlight_01", 
            "bg_office_night_01", "bg_home_kitchen_01", "bg_street_rain_01", 
            "bg_train_compartment_01", "bg_car_night_01", "bg_temple_steps_01"
        );
        
        List<String> objects = List.of(
            "obj_old_phone_01", "obj_crumpled_letter_01", "obj_old_photo_01", 
            "obj_broken_mirror_01", "obj_candle_01", "obj_music_box_01", 
            "obj_rusty_key_01", "obj_medicine_bottle_01", "obj_ecg_strip_02", 
            "obj_wristband_01", "obj_school_bag_01", "obj_chalk_piece_01", 
            "obj_friendship_bracelet_01", "obj_broken_ring_01", "obj_locked_diary_01", 
            "obj_torn_ticket_01", "obj_office_id_card_01", "obj_empty_cup_01", 
            "obj_wet_umbrella_01", "obj_bathroom_towel_01", "obj_shadow_handprint_01", 
            "obj_clock_01", "obj_broken_trophy_01", "obj_childhood_toy_01"
        );

        return ResponseEntity.ok(Map.of("backgrounds", backgrounds, "objects", objects));
    }
}
