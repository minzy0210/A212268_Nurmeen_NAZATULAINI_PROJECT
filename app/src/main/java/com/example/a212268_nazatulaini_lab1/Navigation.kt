package com.example.a212268_nazatulaini_lab1

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation(
    viewModel        : ReServeViewModel,
    chatViewModel    : ChatViewModel,
    profileViewModel : ProfileViewModel,
    locationViewModel: LocationViewModel
) {
    val navController = rememberNavController()

    val goHome: () -> Unit = {
        navController.navigate("home") {
            popUpTo("home") { inclusive = false }
        }
    }

    fun navigateToDetail(itemName: String) {
        val userItem = viewModel.getUserListedItem(itemName)
        when {
            userItem != null && userItem.sellerName == "Me" -> {
                val encodedName = Uri.encode(userItem.name)
                val encodedCat  = Uri.encode(userItem.category)
                navController.navigate("my_listing_detail?name=$encodedName&cat=$encodedCat")
            }
            userItem != null && userItem.category.equals("Food", ignoreCase = true) ->
                navController.navigate("foodDetail/${Uri.encode(itemName)}")
            userItem != null ->
                navController.navigate("nonFoodDetail/${Uri.encode(itemName)}")
            viewModel.getFoodItems().any { it.name == itemName } ->
                navController.navigate("foodDetail/${Uri.encode(itemName)}")
            else ->
                navController.navigate("nonFoodDetail/${Uri.encode(itemName)}")
        }
    }

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            ReServeApp(
                onFoodItemClick     = { navigateToDetail(it) },
                onNonFoodItemClick  = { navigateToDetail(it) },
                onCartClick         = { navController.navigate("cart") },
                onAddClick          = { navController.navigate("add_item") },
                onEmailClick        = { owner, item -> navController.navigate("chat_detail/$owner/$item") },
                chatViewModel       = chatViewModel,
                onAllFoodClick      = { navController.navigate("category/Food") },
                onAllNonFoodClick   = { navController.navigate("category/Non-food") },
                onAllGoingSoonClick = { navController.navigate("going_soon") },
                onAllMyListingsClick = { navController.navigate("category/My Listings") },
                onHomeClick         = goHome,
                onProfileClick      = { navController.navigate("profile") },
                viewModel           = viewModel,
                locationViewModel   = locationViewModel
            )
        }

        composable("foodDetail/{itemName}") { back ->
            val itemName = back.arguments?.getString("itemName") ?: ""
            FoodDetailScreen(
                itemName          = itemName,
                onBack            = { navController.popBackStack() },
                onHomeClick       = goHome,
                onMessageOwner    = { owner, item -> navController.navigate("chat_detail/$owner/$item") },
                viewModel         = viewModel,
                chatViewModel     = chatViewModel,
                locationViewModel = locationViewModel
            )
        }

        composable("nonFoodDetail/{itemName}") { back ->
            val itemName = back.arguments?.getString("itemName") ?: ""
            NonFoodDetailScreen(
                itemName          = itemName,
                onBack            = { navController.popBackStack() },
                onHomeClick       = goHome,
                onMessageOwner    = { owner, item -> navController.navigate("chat_detail/$owner/$item") },
                viewModel         = viewModel,
                chatViewModel     = chatViewModel
            )
        }

        composable("cart") {
            CartScreen(
                onBack      = { navController.popBackStack() },
                onHomeClick = goHome,
                viewModel   = viewModel
            )
        }

        composable("chat_detail/{ownerName}/{itemName}") { back ->
            val owner = back.arguments?.getString("ownerName") ?: ""
            val item  = back.arguments?.getString("itemName") ?: ""
            ChatDetailScreen(
                ownerName     = owner,
                itemName      = item,
                onBack        = { navController.popBackStack() },
                onHomeClick   = goHome,
                chatViewModel = chatViewModel
            )
        }

        composable("add_item") {
            AddItemScreen(
                onBack      = { navController.popBackStack() },
                onHomeClick = goHome,
                onViewItem  = { name, cat ->
                    val encodedName = Uri.encode(name)
                    val encodedCat  = Uri.encode(cat)
                    navController.navigate("my_listing_detail?name=$encodedName&cat=$encodedCat")
                },
                viewModel = viewModel
            )
        }

        composable("going_soon") {
            GoingSoonScreen(
                onBack      = { navController.popBackStack() },
                onItemClick = { navigateToDetail(it) },
                onHomeClick = goHome,
                viewModel   = viewModel
            )
        }

        // ── My listing detail ─────────────────────────────────────────
        composable(
            route = "my_listing_detail?name={itemName}&cat={category}",
            arguments = listOf(
                navArgument("itemName") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("category") {
                    type = NavType.StringType
                    defaultValue = "Food"
                }
            )
        ) { back ->
            val name = Uri.decode(back.arguments?.getString("itemName") ?: "")
            val cat  = Uri.decode(back.arguments?.getString("category") ?: "Food")
            MyListingDetailScreen(
                itemName    = name,
                category    = cat,
                onBack      = { navController.popBackStack() },
                onHomeClick = goHome,
                onDeleted   = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onEdit = {
                    // Navigate to the edit screen for this listing
                    val encodedName = Uri.encode(name)
                    navController.navigate("edit_listing?name=$encodedName")
                },
                viewModel = viewModel
            )
        }

        // ── Edit listing ──────────────────────────────────────────────
        composable(
            route = "edit_listing?name={itemName}",
            arguments = listOf(
                navArgument("itemName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { back ->
            val name = Uri.decode(back.arguments?.getString("itemName") ?: "")
            EditListingScreen(
                itemName = name,
                onBack   = { navController.popBackStack() },
                onSaved  = { newName, cat ->
                    // Pop edit screen + detail screen, then open the updated detail
                    navController.popBackStack()
                    navController.popBackStack()
                    val encodedName = Uri.encode(newName)
                    val encodedCat  = Uri.encode(cat)
                    navController.navigate("my_listing_detail?name=$encodedName&cat=$encodedCat")
                },
                viewModel = viewModel
            )
        }

        composable("category/{filter}") { back ->
            val filter = back.arguments?.getString("filter") ?: "Food"
            CategoryScreen(
                filter             = filter,
                onBack             = { navController.popBackStack() },
                onHomeClick        = goHome,
                onFoodItemClick    = { navigateToDetail(it) },
                onNonFoodItemClick = { navigateToDetail(it) },
                viewModel          = viewModel
            )
        }

        composable("profile") {
            ProfileScreen(
                onBack            = { navController.popBackStack() },
                onHomeClick       = goHome,
                profileViewModel  = profileViewModel,
                locationViewModel = locationViewModel
            )
        }
    }
}