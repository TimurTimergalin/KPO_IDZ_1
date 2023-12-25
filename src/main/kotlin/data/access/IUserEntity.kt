package data.access

interface IUserEntity : IDataEntity{
    val login: String
    val password: String
    val isAdmin: Boolean
}